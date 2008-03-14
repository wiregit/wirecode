package org.limewire.xmpp.client.commands;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

public class CommandDispatcher implements Runnable{
    final HashMap<String, Command> cmds;
    protected BufferedReader reader;

    public CommandDispatcher() {
        cmds = new HashMap<String, Command>();
        reader = new BufferedReader(new InputStreamReader(System.in));
    }
    
    public void add(Command cmd){
        cmds.put(cmd.getCommand(), cmd);
    }
    
    public void run() {
        while(true) {
            try {
                String line = reader.readLine();
                execute(line);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }        
    }

    private void execute(String cmd) {
        StringTokenizer st = new StringTokenizer(cmd);
        Command command = cmds.get(st.nextToken());
        if(command != null) {
            cmd = cmd.substring(command.getCommand().length());
            cmd = cmd.trim();
            try {
                command.execute(cmd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                cmds.get(Command.DEFAULT).execute(cmd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
