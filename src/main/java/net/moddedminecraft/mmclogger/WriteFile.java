package net.moddedminecraft.mmclogger;


import org.spongepowered.api.scheduler.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

class WriteFile implements Consumer<Task> {

    private File type;
    private String[] i;

    WriteFile(String[] index, File t) {
        this.type = t;
        this.i = index;
    }

    @Override
    public void accept(Task task) {
        File log = this.type;
        BufferedWriter buffwriter;
        FileWriter filewriter;
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
        catch (IOException ignored) {}

    }
}
