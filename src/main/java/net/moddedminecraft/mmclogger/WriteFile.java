package net.moddedminecraft.mmclogger;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WriteFile {

    public File type;
    public String[] i;

    WriteFile(String[] index, File t)
    {
        this.type = t;
        this.i = index;
    }

    public void run() {
        File log = this.type;
        BufferedWriter buffwriter = null;
        FileWriter filewriter = null;
        try {
            filewriter = new FileWriter(log, true);
            buffwriter = new BufferedWriter(filewriter);

            for (String s : this.i) {
                buffwriter.write(s);
                buffwriter.newLine();
            }

            buffwriter.flush();

        }
        catch (IOException e) {}
    }
}
