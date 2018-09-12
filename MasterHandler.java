import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;

/*This class loads the script and initializes everything the user will interact with.
The script file is provided as a program argument.
* Please see the documentation.txt file to learn how to use script files*/
public class MasterHandler implements KeyListener, MouseListener{
    private static InstructionParser parser;
    private static Scanner instructions;
    private static ArrayList<String> instructionList = new ArrayList<>();

    public static void main(String[] args) {
        DrawingPanel dp = new DrawingPanel(800, 600); //800x600 ought to be enough for anybody.
        dp.addKeyListener(new MasterHandler());
        dp.addMouseListener(new MasterHandler());
        Graphics g = dp.getGraphics();
        parser = new InstructionParser(g);
        try {
            instructions = new Scanner(new File(args[0])); //find and load the script
        } catch (FileNotFoundException e){
            System.out.println("The script file is missing!");
            System.exit(2);
        }

        while (instructions.hasNextLine()) {
            instructionList.add(instructions.nextLine()); //load instructions onto ArrayList instructions
        }

        doNextInstruction('a'); //Start the program
    }

    //Has the parser do the next instruction, and if it should auto advance, do the next next one as well
    public static void doNextInstruction(char keyChar){
        while (nextIsImport(instructionList.get(0))) {}

        if (parser.getMenuState()) {  //if and only if the parser expects an integer...
            if (Character.isDigit(keyChar) &&  //...do the next instruction if the keyChar is an integer
                    Character.getNumericValue(keyChar) <= parser.getMenuItemsSize() &&
                    Character.getNumericValue(keyChar) > 0){
                while (parser.parseInstruction(instructionList.remove(0), keyChar)) {nextIsImport(instructionList.get(0));} //Causes auto-advancing instr. to advance
                return;
            }
            return;
        }

        while (parser.parseInstruction(instructionList.remove(0), keyChar)) {nextIsImport(instructionList.get(0));}
    }

    //Checks if the instruction is an IMPORT, which cannot be handled by InstructionParser
    public static Boolean nextIsImport(String instruction){
        if (instruction.split(" ")[0].equals("IMPORT")){
            importScript(instruction.split(" ")[1]);
            instructionList.remove(0);
            return true;
        }
        return false;
    }

    //Appends an imported script to the current ArrayList of instructions
    public static void importScript(String filename){
        try {
            Scanner foreignScript = new Scanner(new File(filename));

            instructionList.add("LBL imported_" + filename);
            while (foreignScript.hasNextLine()){
                instructionList.add(foreignScript.nextLine());
            }

        } catch (FileNotFoundException e){
            System.out.println("The script file is missing!");
            System.exit(3);
        }
    }

    //These handle mouse and keyboard input. They do nothing but run doNextInstruction() and pass the pressed key

    @Override
    public void keyReleased(KeyEvent k){
        doNextInstruction(k.getKeyChar());
    }

    @Override
    public void mouseReleased(MouseEvent e){
        doNextInstruction('m');
    }

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e){}

    @Override
    public void mouseExited(MouseEvent e){}

    @Override
    public void mouseClicked(MouseEvent e){}

    @Override
    public void mousePressed(MouseEvent e){}
}
