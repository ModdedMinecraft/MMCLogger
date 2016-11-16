package net.moddedminecraft.mmclogger;


import org.spongepowered.api.scheduler.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

public class WriteFile implements Consumer<Task> {

    public File type;
    public String[] i;

    WriteFile(String[] index, File t)
    {
        this.type = t;
        this.i = index;
    }

    @Override
    public void accept(Task task) {
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
            task.cancel();

        }
        catch (IOException e) {}

    }
}
