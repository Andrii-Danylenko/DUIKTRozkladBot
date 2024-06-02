package org.rozkladbot.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class AdminCommandHandler implements Runnable {
    private static final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    @Override
    public void run() {
        AdminCommands.getAllCommands();
        try {
            listenCommand();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void listenCommand() throws IOException {
        while (true) {
            String command = "";
            try {
                System.out.print("Введіть команду: ");
                command = bufferedReader.readLine();
                System.out.println();
            } catch (IOException exception) {
                System.out.println("Помилка під час зчитування команди");
            }
            if (command.equalsIgnoreCase("/viewUsers")) {
                System.out.println(AdminCommands.viewUsers());
            } else if (command.startsWith("/synchronize ")) {
                AdminCommands.synchronize(command.split("\\s"));
            } else if (command.equalsIgnoreCase("/terminateSession")) {
                AdminCommands.synchronize("/synchronize", "-all");
                System.exit(0);
            } else if (command.equalsIgnoreCase("/commands")) {
                AdminCommands.getAllCommands();
            } else if (command.equalsIgnoreCase("/forceFetch")) {
                AdminCommands.forceFetch();
            } else if (command.toLowerCase().startsWith("/sendMessage".toLowerCase())) {
                AdminCommands.sendMessage(command);
            } else if (command.equalsIgnoreCase("/currentDate")) {
                AdminCommands.getCurrentDate();
            }
            else System.out.println("Невідома команда.");
        }
    }

}
