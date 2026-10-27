// Replaces Electron's single numeric icon group in a disposable packaging copy.
// Compile with the Windows .NET Framework csc.exe; no network or GUI required.
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.IO;
using System.Runtime.InteropServices;

internal static class SetWindowsIcon
{
    private const int RT_ICON = 3;
    private const int RT_GROUP_ICON = 14;
    private delegate bool NameCallback(IntPtr module, IntPtr type, IntPtr name, IntPtr param);
    private delegate bool LanguageCallback(IntPtr module, IntPtr type, IntPtr name, ushort language, IntPtr param);

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern IntPtr LoadLibraryEx(string path, IntPtr file, uint flags);
    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool FreeLibrary(IntPtr module);
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool EnumResourceNames(IntPtr module, IntPtr type, NameCallback callback, IntPtr param);
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool EnumResourceLanguages(IntPtr module, IntPtr type, IntPtr name, LanguageCallback callback, IntPtr param);
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern IntPtr BeginUpdateResource(string path, bool deleteExistingResources);
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool UpdateResource(IntPtr update, IntPtr type, IntPtr name, ushort language, byte[] data, uint size);
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool EndUpdateResource(IntPtr update, bool discard);

    private static ushort U16(byte[] data, int offset) { return BitConverter.ToUInt16(data, offset); }
    private static uint U32(byte[] data, int offset) { return BitConverter.ToUInt32(data, offset); }
    private static void Require(bool condition, string message) { if (!condition) throw new InvalidDataException(message); }
    private static Exception WinError(string message) { return new Win32Exception(Marshal.GetLastWin32Error(), message); }

    private static void Main(string[] args)
    {
        try
        {
            Require(args.Length == 2, "Usage: set-windows-icon.exe <packaging-copy.exe> <icon.ico>");
            byte[] ico = File.ReadAllBytes(args[1]);
            Require(ico.Length >= 6 && U16(ico, 0) == 0 && U16(ico, 2) == 1, "Invalid ICO header.");
            int count = U16(ico, 4);
            Require(count > 0 && count <= 32 && ico.Length >= 6 + count * 16, "Invalid ICO entries.");
            var images = new List<byte[]>();
            for (int i = 0; i < count; i++)
            {
                int at = 6 + i * 16;
                uint length = U32(ico, at + 8), offset = U32(ico, at + 12);
                Require(length > 0 && offset >= 6 + count * 16 && (long)offset + length <= ico.Length, "ICO entry outside file.");
                byte[] bytes = new byte[length];
                Buffer.BlockCopy(ico, (int)offset, bytes, 0, (int)length);
                images.Add(bytes);
            }

            IntPtr module = LoadLibraryEx(args[0], IntPtr.Zero, 2); // LOAD_LIBRARY_AS_DATAFILE
            if (module == IntPtr.Zero) throw WinError("Cannot inspect executable resources.");
            var used = new HashSet<int>();
            var groups = new List<long>();
            var languages = new List<ushort>();
            try
            {
                NameCallback icons = delegate(IntPtr m, IntPtr t, IntPtr n, IntPtr p) { if (n.ToInt64() <= 65535) used.Add((int)n.ToInt64()); return true; };
                NameCallback iconGroups = delegate(IntPtr m, IntPtr t, IntPtr n, IntPtr p) { groups.Add(n.ToInt64()); return true; };
                if (!EnumResourceNames(module, (IntPtr)RT_ICON, icons, IntPtr.Zero)) throw WinError("Cannot enumerate icon resources.");
                if (!EnumResourceNames(module, (IntPtr)RT_GROUP_ICON, iconGroups, IntPtr.Zero)) throw WinError("Cannot enumerate icon groups.");
                Require(groups.Count == 1 && groups[0] == 1, "Unexpected executable icon groups; refusing to guess which to replace.");
                LanguageCallback langs = delegate(IntPtr m, IntPtr t, IntPtr n, ushort l, IntPtr p) { languages.Add(l); return true; };
                if (!EnumResourceLanguages(module, (IntPtr)RT_GROUP_ICON, (IntPtr)1, langs, IntPtr.Zero)) throw WinError("Cannot enumerate icon languages.");
                Require(languages.Count > 0, "No icon group language.");
            }
            finally { FreeLibrary(module); }

            ushort[] ids = new ushort[count];
            int candidate = 1;
            for (int i = 0; i < count; i++)
            {
                while (candidate <= 65535 && used.Contains(candidate)) candidate++;
                Require(candidate <= 65535, "No free icon resource IDs.");
                ids[i] = (ushort)candidate++;
            }
            byte[] group = new byte[6 + 14 * count];
            Buffer.BlockCopy(ico, 0, group, 0, 6);
            for (int i = 0; i < count; i++)
            {
                Buffer.BlockCopy(ico, 6 + 16 * i, group, 6 + 14 * i, 12);
                Buffer.BlockCopy(BitConverter.GetBytes(ids[i]), 0, group, 6 + 14 * i + 12, 2);
            }
            IntPtr update = BeginUpdateResource(args[0], false);
            if (update == IntPtr.Zero) throw WinError("Cannot update executable resources.");
            bool commit = false;
            try
            {
                foreach (ushort language in languages)
                {
                    for (int i = 0; i < count; i++)
                        if (!UpdateResource(update, (IntPtr)RT_ICON, (IntPtr)ids[i], language, images[i], (uint)images[i].Length)) throw WinError("Cannot write icon image.");
                    if (!UpdateResource(update, (IntPtr)RT_GROUP_ICON, (IntPtr)1, language, group, (uint)group.Length)) throw WinError("Cannot write icon group.");
                }
                commit = true;
            }
            finally { if (!EndUpdateResource(update, !commit)) throw WinError("Cannot finish resource update."); }
            Console.WriteLine("Updated {0} icon sizes in {1} language(s).", count, languages.Count);
        }
        catch (Exception error) { Console.Error.WriteLine("Icon update failed: " + error.Message); Environment.ExitCode = 1; }
    }
}
