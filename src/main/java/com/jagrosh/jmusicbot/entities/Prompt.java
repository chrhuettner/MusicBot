/*
 * Copyright 2018 John Grosh (jagrosh)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.entities;

import java.util.Scanner;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Prompt
{
    private final String title;
    private final String noguiMessage;
    
    private boolean nogui;
    private final boolean noprompt;
    private Scanner scanner;

    private static Prompt prompt;

    public static Prompt getInstance(){
        if(prompt == null){
            prompt = new Prompt("JMusicBot");
        }
        return prompt;
    }
    private Prompt(String title)
    {
        this(title, null);
    }

    private Prompt(String title, String noguiMessage)
    {
        this(title, noguiMessage, "true".equalsIgnoreCase(System.getProperty("nogui")), "true".equalsIgnoreCase(System.getProperty("noprompt")));
    }
    
    public Prompt(String title, String noguiMessage, boolean nogui, boolean noprompt)
    {
        this.title = title;
        this.noguiMessage = noguiMessage == null ? "Switching to nogui mode. You can manually start in nogui mode by including the -Dnogui=true flag." : noguiMessage;
        this.nogui = nogui;
        this.noprompt = noprompt;
    }
    
    public boolean isNoGUI()
    {
        return nogui;
    }

    public void alert(Level level, String context, String message) {
        if (nogui) {
            logMessage(level, context, message);
        } else {
            showGuiAlert(level, message, context);
        }
    }

    private void logMessage(Level level, String context, String message) {
        Logger log = LoggerFactory.getLogger(context);
        switch (level) {
            case WARNING:
                log.warn(message);
                break;
            case ERROR:
                log.error(message);
                break;
            case INFO:
            default:
                log.info(message);
                break;
        }
    }

    private void showGuiAlert(Level level, String message, String context) {
        try {
            int option = getGuiMessageType(level);
            JOptionPane.showMessageDialog(null, "<html><body><p style='width: 400px;'>" + message, title, option);
        } catch (Exception e) {
            handleGuiFailure(level, context, message);
        }
    }

    private int getGuiMessageType(Level level) {
        switch (level) {
            case INFO:
                return JOptionPane.INFORMATION_MESSAGE;
            case WARNING:
                return JOptionPane.WARNING_MESSAGE;
            case ERROR:
                return JOptionPane.ERROR_MESSAGE;
            default:
                return JOptionPane.PLAIN_MESSAGE;
        }
    }

    private void handleGuiFailure(Level level, String context, String message) {
        nogui = true;
        alert(Level.WARNING, context, noguiMessage);
        alert(level, context, message);
    }

    
    public String prompt(String content)
    {
        if(noprompt)
            return null;
        if(nogui)
        {
            if(scanner==null)
                scanner = new Scanner(System.in);
            try
            {
                System.out.println(content);
                if(scanner.hasNextLine())
                    return scanner.nextLine();
                return null;
            }
            catch(Exception e)
            {
                alert(Level.ERROR, title, "Unable to read input from command line.");
                e.printStackTrace();
                return null;
            }
        }
        else
        {
            try 
            {
                return JOptionPane.showInputDialog(null, content, title, JOptionPane.QUESTION_MESSAGE);
            }
            catch(Exception e) 
            {
                nogui = true;
                alert(Level.WARNING, title, noguiMessage);
                return prompt(content);
            }
        }
    }
    
    public static enum Level
    {
        INFO, WARNING, ERROR;
    }
}
