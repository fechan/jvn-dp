import java.awt.*;
import java.util.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.lang.System;

/*This class parses and executes script instructions*/
public class InstructionParser{
    private Graphics mainGraphics;

    private Map<String, Boolean> flags = new HashMap<>();

    private Color themeBackgroundColor = Color.CYAN;
    private Color themeTextColor = Color.BLACK;
    private String currentBGFile;
    private String currentFGLeftFile;
    private int[] marginsLeft = {0,0};
    private String currentFGRightFile;
    private int[] marginsRight = {0,0};

    private String currentLabel;

    private ArrayList<String[]> menuItems = null;
    private boolean inMenuState = false;

    private AudioHandler audio;

    public InstructionParser(Graphics dpGraphics){
        this.mainGraphics = dpGraphics;
    }

    //Calls the appropriate private method depending on the instruction given and returns whether or not it should auto-
    //advance
    public boolean parseInstruction(String instruction, char keyPressed){
        String[] arguments = instruction.trim().split(" ");
        arguments[0] = arguments[0].toUpperCase();

        if ((arguments[0].equals("LBL") || arguments[0].equals("LABEL")) && arguments[1].equals(currentLabel)) {
            currentLabel = null;
            return true;
        } else if (inMenuState) {
            if (Character.isDigit(keyPressed)) {
                inMenuState = false;
                currentLabel = menuItems.get(Character.getNumericValue(keyPressed) - 1)[0];
                resetScreen();
            } else {
                System.out.println("Noninteger key was passed when expected during MENU!");
                System.exit(1); //MasterHandler should prevent a noninteger key from going through if in a menu state
                                //otherwise this will trigger
            }
        }

        if (currentLabel == null && !inMenuState) {
            if (!arguments[0].equals("TXTC") && !(arguments[0].equals("IF"))){
                resetScreen();
            }

            switch (arguments[0]) {
                case "CLR":
                case "CLEAR":
                    resetScreen();
                    return true;
                case "COLOR":
                    setThemeColor(Integer.parseInt(arguments[1]), Integer.parseInt(arguments[2]), Integer.parseInt(arguments[3]),
                            Integer.parseInt(arguments[4]), Integer.parseInt(arguments[5]), Integer.parseInt(arguments[6]));
                    return true;
                case "TXT":
                case "TEXT":
                    drawTextboxRegular(String.join(" ", Arrays.copyOfRange(arguments, 1, arguments.length)));
                    return false;
                case "TXTC":
                    drawTextContinuing(Integer.parseInt(arguments[1]), String.join(" ", Arrays.copyOfRange(arguments, 2, arguments.length)));
                    return false;
                case "DLG":
                case "DIALOG":
                    String[] dialogInfo = String.join(" ", Arrays.copyOfRange(arguments, 1, arguments.length)).split(":");
                    drawTextboxDialog(dialogInfo[0], dialogInfo[1]);
                    return false;
                case "BG":
                case "BACKGROUND":
                    setBackground(arguments[1]);
                    return true;
                case "LBL":
                case "LABEL":
                    return true;
                case "JMP":
                case "JUMPTO":
                case "JUMP":
                    currentLabel = arguments[1];
                    return true;
                case "MENU":
                    menuItems = new ArrayList<>();
                    return true;
                case "OPTION":
                    menuItems.add(Arrays.copyOfRange(arguments, 1, arguments.length));
                    return true;
                case "ENDMENU":
                    drawMenu();
                    inMenuState = true;
                    return false;
                case "FGLEFT":
                case "FOREGROUNDLEFT":
                    setForeground(arguments[1], Integer.parseInt(arguments[2]), Integer.parseInt(arguments[3]), true);
                    return true;
                case "FGRIGHT":
                case "FOREGROUNDRIGHT":
                    setForeground(arguments[1], Integer.parseInt(arguments[2]), Integer.parseInt(arguments[3]), false);
                    return true;
                case "FLAG":
                    flags.put(arguments[1], true);
                    return true;
                case "UNFLAG":
                    flags.replace(arguments[1], false);
                    return true;
                case "IF":
                    if (flags.get(arguments[1]) == Boolean.TRUE){
                        return parseInstruction(String.join(" ", Arrays.copyOfRange(arguments, 2, arguments.length)), 'a');
                    }
                    return true;
                case "IMPORT":
                    return true;
                case "PLAY":
                    playMusic(Double.parseDouble(arguments[1])/100, String.join(" ", Arrays.copyOfRange(arguments, 2, arguments.length)));
                    return true;
                case "STOPPLAY":
                case "STOPPLAYBACK":
                    stopPlayback();
                    return true;
                case "STOP":
                    System.exit(0);
            }
        }
        return true;
    }

    //Resets the screen to contain only the background
    private void resetScreen(){
        mainGraphics.setColor(Color.white);
        mainGraphics.fillRect(0, 0, 800, 600);
        if (this.currentBGFile != null) {
            setBackground(this.currentBGFile);
        }

        if (this.currentFGLeftFile != null) {
            setForeground(this.currentFGLeftFile, marginsLeft[0], marginsLeft[1], true);
        }
        if (this.currentFGRightFile != null) {
            setForeground(this.currentFGRightFile, marginsRight[0], marginsRight[1], false);
        }

    }

    //Draws a generic textbox with text
    private void drawTextboxRegular(String text){
        mainGraphics.setColor(themeBackgroundColor);
        mainGraphics.fillRect(10, 430, 780, 160);
        mainGraphics.setColor(this.themeTextColor);
        mainGraphics.drawString(text, 20, 450);
    }

    //Draws additional continuing text under a previous TXT
    private void drawTextContinuing(int line, String text){
        mainGraphics.setColor(this.themeTextColor);
        mainGraphics.translate(0, 20*line);
        mainGraphics.drawString(text, 20, 450);
        mainGraphics.translate(0, -20*line);
    }

    //Draws a textbox, but with a nametag on top
    private void drawTextboxDialog(String name, String text){
        drawNametag(name);
        drawTextboxRegular(text);
    }

    //Draws a nametag
    private void drawNametag(String name){
        mainGraphics.setColor(themeBackgroundColor);
        int nametagSize = 20 + name.length()*6;
        mainGraphics.fillRect(10, 405, nametagSize, 20);
        mainGraphics.setColor(this.themeTextColor);
        mainGraphics.drawString(name, 20, 420);
    }

    //Sets the background
    private void setBackground(String filename){
        this.currentBGFile = null;
        resetScreen();
        this.currentBGFile = filename;
        BufferedImage bgImage;
        try {
            bgImage = ImageIO.read(new File(filename));
            mainGraphics.drawImage(bgImage, 0, 0, Color.WHITE, null);
        } catch (IOException e){
            if (!filename.equals("null")){ System.out.println("Unable to set BG");}
        }
    }

    //Sets the foreground with margins from left or right depending on what fromLeft is specified as
    private void setForeground(String filename, int marginSide, int marginTop, boolean fromLeft){
        if (fromLeft){
            this.currentFGLeftFile = null;
        } else {
            this.currentFGRightFile = null;
        }
        resetScreen();
        if (fromLeft){
            this.marginsLeft[0] = marginSide;
            this.marginsLeft[1] = marginTop;
            this.currentFGLeftFile = filename;
        } else {
            this.marginsRight[0] = marginSide;
            this.marginsRight[1] = marginTop;
            this.currentFGRightFile = filename;
        }
        BufferedImage fgImage;
        try {
            fgImage = ImageIO.read(new File(filename));
            mainGraphics.drawImage(fgImage, fromLeft ? marginSide:800-marginSide, marginTop, Color.WHITE, null);
        } catch (IOException e){
            if (!filename.equals("null")){ System.out.println("Unable to set FG");}
        }
    }

    //Causes a menu to show
    private void drawMenu() {
        int marginLeft = 200;
        int marginTop = 10;

        for (int opt = 0; opt < menuItems.size(); opt++){
            mainGraphics.setColor(themeBackgroundColor);
            mainGraphics.fillRect(marginLeft, opt*30+marginTop, 400, 20);

            mainGraphics.setColor(this.themeTextColor);
            mainGraphics.drawString(opt+1 + ": " + String.join(" ", Arrays.copyOfRange(menuItems.get(opt), 1, menuItems.get(opt).length
                )), marginLeft+10, opt*30+15+marginTop);
        }
    }

    //causes music to play
    private void playMusic(double volume ,String fileName){
        this.audio = new AudioHandler(fileName);
        this.audio.startMusic();
        this.audio.setVolume(volume);
    }

    //causes music to stop
    private void stopPlayback(){
        try {
            this.audio.stopMusic();
        } catch (NullPointerException e){
            System.out.println("Unable to stop the music! Is a sick jam not currently playing?");
        }
    }

    //sets the color of all the UI elements to the desired RGB value
    private void setThemeColor(int backgroundR, int backgroundG, int backgroundB, int textR, int textG, int textB){
        this.themeBackgroundColor = new Color(backgroundR, backgroundG, backgroundB);
        this.themeTextColor = new Color(textR, textG, textB);
    }

    //gets whether or not InstructionParser is in a menu state and expects an integer key input
    public boolean getMenuState(){
        return inMenuState;
    }

    //gets how many items are in the current menu
    public int getMenuItemsSize(){ return this.menuItems.size(); }
}
