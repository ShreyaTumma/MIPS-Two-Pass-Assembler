//Name: Gabriel Barney and Shreya Tumma
//Section: CPE 315-03
//Description: Two Pass Assembler

import java.io.*;
import java.util.*;


class lab2 {
    
    // starting address: 0x00400000, but for this assignment start at zero
    // any instr with opcode of 000000 is of R-type
    static Map<String, String> opCodes = new HashMap<String, String>() {{
        put("and", "000000");
        put("add", "000000"); 
        put("or", "000000");
        put("sll", "000000");
        put("sub", "000000");
        put("slt", "000000");
        put("jr", "000000");
        put("addi", "001000");
        put("beq", "000100");
        put("bne", "000101");
        put("lw", "100011");
        put("sw", "101011");
        put("j", "000010");
        put("jal", "000011"); 
    }};

    static Map<String, String> functionCodes = new HashMap<String, String>() {{
        put("and", "00000 100100");
        put("add", "00000 100000");
        put("or", "00000 100101");
        put("addi", "immediate");
        put("sll", "sa000000"); // this one
        put("sub", "00000 100010");
        put("slt", "00000 101010");
        put("beq", "offset");
        put("bne", "offset");
        put("lw", "offset");
        put("sw", "offset");
        put("j", "target");
        put("jr", "001000"); // lots of zeroes before, always this value? ask
        put("jal", "target");
    }};

    static Map<String, String> regCodes = new HashMap<String, String>() {{
        put("0", "00000");
        put("zero", "00000");
        put("v0", "00010");
        put("v1", "00011");
        put("a0", "00100");
        put("a1", "00101");
        put("a2", "00110");
        put("a3", "00111");
        put("t0", "01000");
        put("t1", "01001");
        put("t2", "01010");
        put("t3", "01011");
        put("t4", "01100");
        put("t5", "01101");
        put("t6", "01110");
        put("t7", "01111");
        put("s0", "10000");
        put("s1", "10001");
        put("s2", "10010");
        put("s3", "10011");
        put("s4", "10100");
        put("s5", "10101");
        put("s6", "10110");
        put("s7", "10111");
        put("t8", "11000");
        put("t9", "11001");
        put("sp", "11101");
        put("ra", "11111");
    }};

    public static void main(String args[]) {
    
        Map<String, String> labels = new HashMap<String, String>();
        ArrayList<String> mCodes = new ArrayList<String>();
        try {
            File file = new File(args[0]);
            FileReader fread = new FileReader(file);
            BufferedReader bread = new BufferedReader(fread);
            StringBuffer buff = new StringBuffer();
            String line;
            int hexAddress = -1;
            ArrayList< ArrayList<String>> instructions = new ArrayList<ArrayList<String> >();

            while ((line = bread.readLine()) != null) {
                hexAddress = getLabelAddresses(line, hexAddress, labels, instructions);
                buff.append(line); //adds to buffer
                buff.append('\n'); 
            }
            fread.close();

            mCodes = printMachineCode(labels, instructions, mCodes);
            for(int i = 0; i < mCodes.size(); i++){
                System.out.print(mCodes.get(i) + "\n");
            }
    
        } catch(IOException e) { 
            e.printStackTrace(); 
        }
    
    }

    // method for first pass
    public static int getLabelAddresses(String line, int hexAddress, 
        Map<String, String> labels, ArrayList<ArrayList <String>> instructions) {
        
        int commentFlag = 0; 
        String[] splitLine = line.split("\\s+|,|\\$");
        ArrayList<String> instr = new ArrayList<String>();

        // Check for empty lines and comments
        if (splitLine != null || splitLine[0].contains("#") == false) {
            for(int i = 0; i < splitLine.length; i++){
                if (commentFlag == 1) { 
                    break;
                }
                // check for comments in same line as instruction                       
                if(splitLine[i].contains("#") == true) {
                    if (splitLine[i].indexOf("#") > 0) {
                        splitLine[i] = splitLine[i].substring(0, splitLine[i].indexOf("#"));
                        commentFlag = 1;
                    } else { 
                        break;
                    }
                }
                if(splitLine[i].length() > 0 ){
                    if (opCodes.containsKey(splitLine[i]) == true) {
                        hexAddress += 1; //add hex address as a value to key (?)
                    }
                    // check for labels and store address
                    int indexCol = splitLine[i].indexOf(":");
                    if(indexCol > -1){
                        labels.put(splitLine[i].substring(0,indexCol), Integer.toString(hexAddress + 1));
                        if(indexCol != splitLine[i].length() - 1){
                            hexAddress += 1;
                            instr.add(splitLine[i].substring(indexCol +1));
                        }
                        continue;
                    } 
                    instr.add(splitLine[i]);
                }
            }
            if(instr.isEmpty() == false){
                instructions.add(instr);
            }  
        }
        return hexAddress;
    }

    // method used for second pass
    public static ArrayList<String> printMachineCode(Map<String, String> labels, 
        ArrayList<ArrayList <String>> instructions, ArrayList<String> mCodes) {
        
        // i is the address of the current instruction
        for(int i = 0; i < instructions.size(); i++){
            String mCode = "";
            ArrayList<String> line = instructions.get(i);
            String current_inst = line.get(0);
            
            switch (line.size()) {
                case 4:
                    if(opCodes.containsKey(current_inst) == true){
                        if(functionCodes.get(current_inst) == "immediate" || functionCodes.get(current_inst) == "offset" ) {
                            mCode += iType(line, current_inst, labels, i);
                        } else if(functionCodes.get(current_inst).contains("sa") == true) {
                            mCode += shift(line, current_inst);
                        } else {
                            mCode += rType(line, current_inst);
                        }
                    }
                    else {
                        mCode += "invalid instruction: " + current_inst;
                        mCodes.add(mCode);
                        return mCodes;
                    }
                    break;
                case 2:
                    String destination = line.get(1);
                    if (current_inst.equals("j") || current_inst.equals("jal")) {   // think about storing pc with jal
                        String binary = Integer.toString(Integer.parseInt(labels.get(destination)), 2);
                        String leadZeroes = String.format("%26s", binary).replace(' ', '0');
                        mCode += opCodes.get(current_inst) + " " + leadZeroes;
                        break;
                    } else if (current_inst.equals("jr")) {
                        mCode += (opCodes.get(current_inst) + " " + 
                                    regCodes.get(destination) + " " +
                                    "000000000000000" + " " +
                                    functionCodes.get(current_inst));
                        break;        
                    } else {
                        mCode += "invalid instruction: " + current_inst;
                        mCodes.add(mCode);
                        return mCodes;
                    }
                default:
                    System.out.println("instruction error");
                }    
                mCodes.add(mCode);  
            }
            return mCodes;
        }

        public static String rType(ArrayList<String> line, String current_inst){
            //ex: add rd, rs, rt           
            String rd = regCodes.get(line.get(1));
            String rs = regCodes.get(line.get(2));
            String rt = regCodes.get(line.get(3));
            String opcode = opCodes.get(current_inst);
            String fCode = functionCodes.get(current_inst);

            return opcode + " " + rs + " " + rt + " " + rd + " " + fCode;
        }

        public static String shift(ArrayList<String> line, String current_inst){
            //ex: add rd, rs, rt    
            String rd = regCodes.get(line.get(1));
            String rs = "00000";
            String rt = regCodes.get(line.get(2));
            String opcode = opCodes.get(current_inst);
            String fCode = "000000";
            String binary = Integer.toBinaryString(Integer.parseInt(line.get(3)));
            String shamt = String.format("%5s",binary ).replace(' ', '0');

            return opcode + " " + rs + " " + rt + " " + rd + " " + shamt + " "+ fCode;
        }

        public static String iType(ArrayList<String> line, String current_inst, Map<String, String> labels, int i){
            String rs = regCodes.get(line.get(1));
            String rt = regCodes.get(line.get(2));
            String opcode = opCodes.get(current_inst);
            String offset = line.get(3);
            int newOff = 0;
                      
            //check to see if label exists 
            if(labels.containsKey(line.get(3)) == true) {
                newOff = Integer.parseInt(labels.get(offset)) - (i+1);
            } else if(offset.contains(")") == true){
                rt = regCodes.get(line.get(1));
                rs = regCodes.get(line.get(3).substring(0,(line.get(3).length())-1));
                offset = line.get(2).substring(0, line.get(2).length()-1);
                newOff = Integer.parseInt(offset);
            } else {
                newOff = Integer.parseInt(offset);
            }
            //check if offset is negative
            if(newOff < 0){
                offset = Integer.toString(twosCompliment(newOff));
            } else{
                offset = Integer.toString(newOff);
            }
                
            String binary = Integer.toBinaryString(Integer.parseInt(offset));
            String im = String.format("%16s", binary).replace(' ', '0');

            return opcode + " " + rs + " " + rt + " " + im ;
        }

        public static int twosCompliment(int offset){
            offset = offset * -1 ;
            int newOffset = (int)(Math.pow(2, 16)) - offset;
            return newOffset;
        }
}
