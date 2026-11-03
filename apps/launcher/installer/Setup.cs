using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Security.Cryptography;
using System.Threading.Tasks;
using System.Windows.Forms;

// The production executable accepts no paths or commands. The test harness is built separately.
internal static class SafeArchive {
    internal static void Extract(Stream source, string destination, string expectedSha256, Func<bool> cancelled) {
        if (Directory.Exists(destination) || File.Exists(destination)) throw new IOException("Destination exists.");
        if (!source.CanSeek || expectedSha256 == null || expectedSha256.Length != 64) throw new InvalidDataException("Missing payload digest.");
        if (cancelled()) throw new OperationCanceledException();
        source.Position = 0;
        string actual;
        using (var sha = SHA256.Create()) actual = BitConverter.ToString(sha.ComputeHash(source)).Replace("-", "").ToLowerInvariant();
        if (!String.Equals(actual, expectedSha256, StringComparison.Ordinal)) throw new InvalidDataException("Payload digest mismatch.");
        source.Position = 0;
        string root = Path.GetFullPath(destination).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
        string parent = Path.GetDirectoryName(Path.GetFullPath(destination));
        for (var directory = new DirectoryInfo(parent); directory != null; directory = directory.Parent) {
            if (directory.Exists && (directory.Attributes & FileAttributes.ReparsePoint) != 0) throw new IOException("Destination contains a link.");
        }
        using (var archive = new ZipArchive(source, ZipArchiveMode.Read, false)) {
            var paths = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            long total = 0;
            if (archive.Entries.Count > 10000) throw new InvalidDataException("Too many files.");
            foreach (var entry in archive.Entries) {
                string name = entry.FullName.Replace('\\', '/');
                if (String.IsNullOrEmpty(name) || name.IndexOf(':') >= 0 || name.StartsWith("/")) throw new InvalidDataException("Invalid archive path.");
                string[] pieces = name.TrimEnd('/').Split('/');
                foreach (string piece in pieces) if (piece == "" || piece == "." || piece == ".." || piece.EndsWith(".") || piece.EndsWith(" ") || System.Text.RegularExpressions.Regex.IsMatch(piece, @"^(con|prn|aux|nul|com[1-9]|lpt[1-9])(\.|$)",System.Text.RegularExpressions.RegexOptions.IgnoreCase)) throw new InvalidDataException("Invalid archive path.");
                string path = Path.GetFullPath(Path.Combine(root, name.Replace('/', Path.DirectorySeparatorChar)));
                if (!path.StartsWith(root, StringComparison.OrdinalIgnoreCase) || !paths.Add(path)) throw new InvalidDataException("Conflicting archive path.");
                int unixType = (entry.ExternalAttributes >> 16) & 0xF000;
                if (unixType != 0 && unixType != 0x8000 && unixType != 0x4000) throw new InvalidDataException("Unsupported archive entry.");
                if (entry.Length > 1024L * 1024 * 1024 || (total += entry.Length) > 2L * 1024 * 1024 * 1024) throw new InvalidDataException("Archive too large.");
            }
            if (cancelled()) throw new OperationCanceledException();
            Directory.CreateDirectory(parent);
            string staging = Path.Combine(parent, ".space-extract-" + Guid.NewGuid().ToString("N"));
            string stageRoot = Path.GetFullPath(staging) + Path.DirectorySeparatorChar;
            try {
            Directory.CreateDirectory(staging);
            foreach (var entry in archive.Entries) {
                if (cancelled()) throw new OperationCanceledException();
                string name = entry.FullName.Replace('\\', '/');
                string path = Path.GetFullPath(Path.Combine(staging, name.Replace('/', Path.DirectorySeparatorChar)));
                if (!path.StartsWith(stageRoot, StringComparison.OrdinalIgnoreCase)) throw new IOException("Invalid staging path.");
                if (name.EndsWith("/")) { Directory.CreateDirectory(path); continue; }
                Directory.CreateDirectory(Path.GetDirectoryName(path));
                using (var input = entry.Open()) using (var output = new FileStream(path, FileMode.CreateNew, FileAccess.Write, FileShare.None)) {
                    byte[] buffer = new byte[65536]; int count; long written = 0;
                    while ((count = input.Read(buffer,0,buffer.Length)) > 0) { if (cancelled()) throw new OperationCanceledException(); written += count; if (written > entry.Length) throw new InvalidDataException("Entry exceeds declared size."); output.Write(buffer,0,count); }
                    if (written != entry.Length) throw new InvalidDataException("Entry size mismatch.");
                    output.Flush(true);
                }
            }
            if (cancelled()) throw new OperationCanceledException();
            Directory.Move(staging,destination);
            } finally {
                // Only our generated sibling staging directory, never destination or user data.
                if (Directory.Exists(staging) && Path.GetDirectoryName(Path.GetFullPath(staging)) == parent && Path.GetFileName(staging).StartsWith(".space-extract-",StringComparison.Ordinal)) Directory.Delete(staging,true);
            }
        }
    }
}

#if TEST_HARNESS
internal static class Program {
    private static int Main(string[] args) {
        if (args.Length < 3 || args.Length > 4) return 2;
        int checks = 0, cancelAfter = args.Length == 4 ? Int32.Parse(args[3]) : -1;
        try { using (var input = File.OpenRead(args[0])) SafeArchive.Extract(input, args[1], args[2], () => cancelAfter >= 0 && checks++ >= cancelAfter); return 0; }
        catch (Exception ex) { Console.Error.WriteLine(ex.Message); return 1; }
    }
}
#else
internal sealed class ExtractionDialog : Form {
    private volatile bool cancelled;
    private bool finished;
    internal Exception Failure;
    internal ExtractionDialog(Stream payload,string target,string hash) {
        Text="Space Client Setup"; Width=420; Height=170; FormBorderStyle=FormBorderStyle.FixedDialog; MaximizeBox=false; MinimizeBox=false; StartPosition=FormStartPosition.CenterScreen;
        var label=new Label {Text="Проверяем и распаковываем лаунчер…",AutoSize=true,Left=20,Top=20};
        var progress=new ProgressBar {Left=20,Top=50,Width=360,Style=ProgressBarStyle.Marquee};
        var cancel=new Button {Text="Отменить",Left=270,Top=85,Width=110};
        Controls.Add(label);Controls.Add(progress);Controls.Add(cancel);
        cancel.Click+=(sender,args)=>{cancelled=true;cancel.Enabled=false;label.Text="Отменяем; существующие данные сохранены…";};
        FormClosing+=(sender,args)=>{if(!finished){cancelled=true;args.Cancel=true;}};
        Shown+=async(sender,args)=>{try {await Task.Run(()=>SafeArchive.Extract(payload,target,hash,()=>cancelled));DialogResult=DialogResult.OK;}catch(OperationCanceledException){DialogResult=DialogResult.Cancel;}catch(Exception error){Failure=error;DialogResult=DialogResult.Abort;}finally{finished=true;Close();}};
    }
}
internal static class Program {
    [STAThread]
    private static void Main() {
        const string title = "Space Client Setup";
        if (MessageBox.Show("Установить development-версию Space Client для текущего пользователя?\n\nФайлы Minecraft и Java не входят; development-мод Space включён. Существующие данные не удаляются.", title, MessageBoxButtons.OKCancel, MessageBoxIcon.Information) != DialogResult.OK) return;
        string staging = null;
        string temporaryLink = null;
        string installedTarget = null;
        try {
            string local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string programs = Environment.GetFolderPath(Environment.SpecialFolder.Programs);
            if (String.IsNullOrEmpty(local) || String.IsNullOrEmpty(programs)) throw new IOException("Не найдены папки текущего пользователя.");
            string parent = Path.Combine(local, "Programs");
            string target = Path.Combine(parent, "Space Client");
            string link = Path.Combine(programs, "Space Client.lnk");
            if (Directory.Exists(target) || File.Exists(target) || File.Exists(link) || Directory.Exists(link)) throw new IOException("Установка или ярлык уже существуют. Переименуйте или удалите их самостоятельно после проверки.");
            Directory.CreateDirectory(parent);
            Directory.CreateDirectory(programs);
            staging = Path.Combine(parent, ".space-setup-" + Guid.NewGuid().ToString("N"));
            using (Stream payload = Assembly.GetExecutingAssembly().GetManifestResourceStream("SpacePayload")) {
                if (payload == null) throw new IOException("В установщике нет файлов приложения.");
                string hash;
                using(Stream digest=Assembly.GetExecutingAssembly().GetManifestResourceStream("SpacePayloadHash")) {
                    if(digest==null)throw new IOException("В установщике нет контрольной суммы.");
                    using(var reader=new StreamReader(digest))hash=reader.ReadToEnd().Trim();
                }
                using(var progress=new ExtractionDialog(payload,staging,hash)) {
                    if(progress.ShowDialog()!=DialogResult.OK) {if(progress.Failure!=null)throw progress.Failure;return;}
                }
            }
            string executable = Path.Combine(target, "Space Client.exe");
            if (!File.Exists(Path.Combine(staging, "Space Client.exe"))) throw new IOException("В установщике отсутствует лаунчер.");
            temporaryLink = Path.Combine(programs, ".space-setup-" + Guid.NewGuid().ToString("N") + ".lnk");
            Type shellType = Type.GetTypeFromProgID("WScript.Shell", true);
            object shell = Activator.CreateInstance(shellType);
            object shortcut = shellType.InvokeMember("CreateShortcut", BindingFlags.InvokeMethod, null, shell, new object[] { temporaryLink });
            Type shortcutType = shortcut.GetType();
            shortcutType.InvokeMember("TargetPath", BindingFlags.SetProperty, null, shortcut, new object[] { executable });
            shortcutType.InvokeMember("WorkingDirectory", BindingFlags.SetProperty, null, shortcut, new object[] { target });
            shortcutType.InvokeMember("IconLocation", BindingFlags.SetProperty, null, shortcut, new object[] { executable + ",0" });
            shortcutType.InvokeMember("Save", BindingFlags.InvokeMethod, null, shortcut, null);
            Directory.Move(staging, target);
            staging = null;
            installedTarget = target;
            File.Move(temporaryLink, link);
            temporaryLink = null;
            MessageBox.Show("Space Client установлен. Ярлык находится в меню Пуск.\n\nЭто development-версия; для игры потребуется Java и отдельная подготовка файлов.", title, MessageBoxButtons.OK, MessageBoxIcon.Information);
        } catch (Exception ex) {
            // Never recursively remove a directory or existing user data after an error.
            string message = "Установка не завершена: " + ex.Message;
            if (installedTarget != null) message += "\nФайлы лаунчера уже размещены в " + installedTarget + ", но ярлык не создан. Проверьте каталог перед повтором установки.";
            if (staging != null) message += "\nВременные файлы: " + staging;
            if (temporaryLink != null) message += "\nВременный ярлык: " + temporaryLink;
            MessageBox.Show(message, title, MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }
}
#endif
