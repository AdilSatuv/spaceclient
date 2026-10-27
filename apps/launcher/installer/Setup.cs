using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Windows.Forms;

// The production executable accepts no paths or commands. The test harness is built separately.
internal static class SafeArchive {
    internal static void Extract(Stream source, string destination) {
        if (Directory.Exists(destination) || File.Exists(destination)) throw new IOException("Destination exists.");
        string root = Path.GetFullPath(destination).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
        using (var archive = new ZipArchive(source, ZipArchiveMode.Read, false)) {
            var paths = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            long total = 0;
            if (archive.Entries.Count > 10000) throw new InvalidDataException("Too many files.");
            foreach (var entry in archive.Entries) {
                string name = entry.FullName.Replace('\\', '/');
                if (String.IsNullOrEmpty(name) || name.IndexOf(':') >= 0 || name.StartsWith("/")) throw new InvalidDataException("Invalid archive path.");
                string[] pieces = name.TrimEnd('/').Split('/');
                foreach (string piece in pieces) if (piece == "" || piece == "." || piece == ".." || piece.EndsWith(".") || piece.EndsWith(" ")) throw new InvalidDataException("Invalid archive path.");
                string path = Path.GetFullPath(Path.Combine(root, name.Replace('/', Path.DirectorySeparatorChar)));
                if (!path.StartsWith(root, StringComparison.OrdinalIgnoreCase) || !paths.Add(path)) throw new InvalidDataException("Conflicting archive path.");
                int unixType = (entry.ExternalAttributes >> 16) & 0xF000;
                if (unixType != 0 && unixType != 0x8000 && unixType != 0x4000) throw new InvalidDataException("Unsupported archive entry.");
                if (entry.Length > 1024L * 1024 * 1024 || (total += entry.Length) > 2L * 1024 * 1024 * 1024) throw new InvalidDataException("Archive too large.");
            }
            Directory.CreateDirectory(destination);
            foreach (var entry in archive.Entries) {
                string name = entry.FullName.Replace('\\', '/');
                string path = Path.GetFullPath(Path.Combine(root, name.Replace('/', Path.DirectorySeparatorChar)));
                if (name.EndsWith("/")) { Directory.CreateDirectory(path); continue; }
                Directory.CreateDirectory(Path.GetDirectoryName(path));
                using (var input = entry.Open()) using (var output = new FileStream(path, FileMode.CreateNew, FileAccess.Write, FileShare.None)) input.CopyTo(output);
            }
        }
    }
}

#if TEST_HARNESS
internal static class Program {
    private static int Main(string[] args) {
        if (args.Length != 2) return 2;
        try { using (var input = File.OpenRead(args[0])) SafeArchive.Extract(input, args[1]); return 0; }
        catch (Exception ex) { Console.Error.WriteLine(ex.Message); return 1; }
    }
}
#else
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
                SafeArchive.Extract(payload, staging);
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
