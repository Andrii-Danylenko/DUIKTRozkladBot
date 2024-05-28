package org.rozkladbot.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AdminCommandHandler implements Runnable {
    private static final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    @Override
    public void run() {
        AdminCommands.getAllCommands();
        listenCommand();
    }


    private void listenCommand() {
        while (true) {
            String command = "";
            try {
                System.out.print("Введіть команду: ");
                command = bufferedReader.readLine();
                System.out.println();
            } catch (IOException exception) {
                System.out.println("Помилка під час зчитування команди");
            }
            if (command.equalsIgnoreCase("/viewusers")) {
                System.out.println(AdminCommands.viewUsers());
            } else if (command.startsWith("/synchronize ")) {
                AdminCommands.synchronize(command.split("\\s"));
            } else if (command.equalsIgnoreCase("/terminatesession")) {
                AdminCommands.synchronize("/synchronize", "-all");
                System.exit(0);
            } else if (command.equalsIgnoreCase("/commands")) {
                AdminCommands.getAllCommands();
            } else if (command.equalsIgnoreCase("/forcefetch")) {
                AdminCommands.forceFetch();
            }
            else System.out.println("Невідома команда.");
        }
    }

}
