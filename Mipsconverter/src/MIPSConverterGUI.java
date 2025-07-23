import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MIPSConverterGUI {
    private static final Map<String, String> registerMap = new HashMap<>();
    private static final Map<String, String> opcodeMap = new HashMap<>();
    private static final Map<String, String> functMap = new HashMap<>();
    private static int currentAddress = 0x00400000;

    static {
        // Initialize register mappings
        registerMap.put("$zero", "00000"); registerMap.put("$0", "00000");
        registerMap.put("$v0", "00010"); registerMap.put("$v1", "00011");
        registerMap.put("$a0", "00100"); registerMap.put("$a1", "00101");
        registerMap.put("$a2", "00110"); registerMap.put("$a3", "00111");
        registerMap.put("$t0", "01000"); registerMap.put("$t1", "01001");
        registerMap.put("$t2", "01010"); registerMap.put("$t3", "01011");
        registerMap.put("$t4", "01100"); registerMap.put("$t5", "01101");
        registerMap.put("$t6", "01110"); registerMap.put("$t7", "01111");
        registerMap.put("$s0", "10000"); registerMap.put("$s1", "10001");
        registerMap.put("$s2", "10010"); registerMap.put("$s3", "10011");
        registerMap.put("$s4", "10100"); registerMap.put("$s5", "10101");
        registerMap.put("$s6", "10110"); registerMap.put("$s7", "10111");
        registerMap.put("$t8", "11000"); registerMap.put("$t9", "11001");
        registerMap.put("$k0", "11010"); registerMap.put("$k1", "11011");
        registerMap.put("$gp", "11100"); registerMap.put("$sp", "11101");
        registerMap.put("$fp", "11110"); registerMap.put("$ra", "11111");

        // Initialize opcode mappings
        opcodeMap.put("add", "000000"); opcodeMap.put("sub", "000000");
        opcodeMap.put("and", "000000"); opcodeMap.put("or", "000000");
        opcodeMap.put("sll", "000000"); opcodeMap.put("srl", "000000");
        opcodeMap.put("sllv", "000000"); opcodeMap.put("srlv", "000000");
        opcodeMap.put("addi", "001000"); opcodeMap.put("andi", "001100");
        opcodeMap.put("lw", "100011"); opcodeMap.put("sw", "101011");
        opcodeMap.put("beq", "000100"); opcodeMap.put("bne", "000101");
        opcodeMap.put("blez", "000110"); opcodeMap.put("bgtz", "000111");
        opcodeMap.put("j", "000010"); opcodeMap.put("jal", "000011");

        // Initialize function mappings for R-type instructions
        functMap.put("add", "100000"); functMap.put("sub", "100010");
        functMap.put("and", "100100"); functMap.put("or", "100101");
        functMap.put("sll", "000000"); functMap.put("srl", "000010");
        functMap.put("sllv", "000100"); functMap.put("srlv", "000110");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MIPSConverterGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("MIPS to Machine Code Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        JLabel inputLabel = new JLabel("Enter MIPS Assembly Code:");
        JTextArea inputArea = new JTextArea(10, 40);
        JScrollPane inputScroll = new JScrollPane(inputArea);

        // Output panel
        JPanel outputPanel = new JPanel(new BorderLayout());
        JLabel outputLabel = new JLabel("Machine Code Output:");
        JTextArea outputArea = new JTextArea(15, 40);
        outputArea.setEditable(false);
        JScrollPane outputScroll = new JScrollPane(outputArea);

        // Button panel
        JPanel buttonPanel = new JPanel();
        JButton convertButton = new JButton("Convert");
        JButton clearButton = new JButton("Clear");

        // Add components to panels
        inputPanel.add(inputLabel, BorderLayout.NORTH);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        outputPanel.add(outputLabel, BorderLayout.NORTH);
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        buttonPanel.add(convertButton);
        buttonPanel.add(clearButton);

        // Add panels to frame
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(outputPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Button actions using lambda expressions
        convertButton.addActionListener(e -> {
            String assemblyCode = inputArea.getText();
            String result = convertAssemblyToMachineCode(assemblyCode);
            outputArea.setText(result);
        });

        clearButton.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
        });

        frame.setVisible(true);
    }

    private static String convertAssemblyToMachineCode(String assemblyCode) {
        StringBuilder result = new StringBuilder();
        currentAddress = 0x00400000;

        // First pass: collect labels and their addresses
        Map<String, Integer> labelMap = new HashMap<>();
        String[] lines = assemblyCode.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Check for label
            if (line.contains(":")) {
                String label = line.substring(0, line.indexOf(":")).trim();
                labelMap.put(label, currentAddress);
                line = line.substring(line.indexOf(":") + 1).trim();
            }

            if (!line.isEmpty()) {
                currentAddress += 4;
            }
        }

        // Second pass: process instructions
        currentAddress = 0x00400000;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Handle label
            if (line.contains(":")) {
                line = line.substring(line.indexOf(":") + 1).trim();
                if (line.isEmpty()) continue;
            }

            String binaryCode = convertInstruction(line, labelMap);
            String hexCode = binaryToHex(binaryCode);

            result.append(String.format("0x%08X", currentAddress))
                    .append(": ")
                    .append(binaryCode)
                    .append(" (")
                    .append(hexCode)
                    .append(")\n");

            currentAddress += 4;
        }

        return result.toString();
    }

    private static String convertInstruction(String instruction, Map<String, Integer> labelMap) {
        instruction = instruction.replaceAll(",", " ").replaceAll("\\s+", " ").trim();
        String[] parts = instruction.split(" ");
        String op = parts[0].toLowerCase();

        if (!opcodeMap.containsKey(op)) {
            return "Invalid instruction: " + op;
        }

        String opcode = opcodeMap.get(op);

        try {
            if (opcode.equals("000000")) { // R-type
                return processRType(parts, op);
            } else if (op.equals("lw") || op.equals("sw")) { // I-type (load/store)
                return processLoadStore(parts, op);
            } else if (op.equals("addi") || op.equals("andi")) { // I-type (immediate)
                return processImmediate(parts, op);
            } else if (op.equals("beq") || op.equals("bne") ||
                    op.equals("blez") || op.equals("bgtz")) { // I-type (branch)
                return processBranch(parts, op, labelMap);
            } else if (op.equals("j") || op.equals("jal")) { // J-type
                return processJump(parts, op, labelMap);
            } else {
                return "Unsupported instruction: " + op;
            }
        } catch (Exception e) {
            return "Error processing instruction: " + instruction;
        }
    }

    private static String processRType(String[] parts, String op) {
        String opcode = "000000";
        String funct = functMap.get(op);

        if (op.equals("sll") || op.equals("srl")) { // Shift with immediate
            String rd = getRegister(parts[1]);
            String rt = getRegister(parts[2]);
            String shamt = String.format("%05d", Integer.parseInt(parts[3]));
            return opcode + "00000" + rt + rd + shamt + funct;
        } else { // Standard R-type
            String rd = getRegister(parts[1]);
            String rs = getRegister(parts[2]);
            String rt = getRegister(parts[3]);
            return opcode + rs + rt + rd + "00000" + funct;
        }
    }

    private static String processLoadStore(String[] parts, String op) {
        String opcode = opcodeMap.get(op);
        String rt = getRegister(parts[1]);

        // Parse offset(base) format
        String[] memParts = parts[2].split("[()]");
        String offsetStr = memParts[0];
        String base = getRegister(memParts[1]);

        int offset = Integer.parseInt(offsetStr);
        String offsetBinary = String.format("%016d", Integer.parseInt(Integer.toBinaryString(offset & 0xFFFF)));

        return opcode + base + rt + offsetBinary;
    }

    private static String processImmediate(String[] parts, String op) {
        String opcode = opcodeMap.get(op);
        String rt = getRegister(parts[1]);
        String rs = getRegister(parts[2]);
        int immediate = Integer.parseInt(parts[3]);

        String immediateBinary;
        if (immediate < 0) {
            immediateBinary = Integer.toBinaryString(immediate).substring(16);
        } else {
            immediateBinary = String.format("%016d", Integer.parseInt(Integer.toBinaryString(immediate)));
        }

        return opcode + rs + rt + immediateBinary;
    }

    private static String processBranch(String[] parts, String op, Map<String, Integer> labelMap) {
        String opcode = opcodeMap.get(op);
        int offset;

        if (op.equals("blez") || op.equals("bgtz")) {
            String rs = getRegister(parts[1]);
            String label = parts[2];
            int targetAddress = labelMap.get(label);
            offset = (targetAddress - (currentAddress + 4)) / 4;
            String offsetBinary = String.format("%016d", Integer.parseInt(Integer.toBinaryString(offset & 0xFFFF)));
            return opcode + rs + "00000" + offsetBinary;
        } else {
            String rs = getRegister(parts[1]);
            String rt = getRegister(parts[2]);
            String label = parts[3];
            int targetAddress = labelMap.get(label);
            offset = (targetAddress - (currentAddress + 4)) / 4;
            String offsetBinary = String.format("%016d", Integer.parseInt(Integer.toBinaryString(offset & 0xFFFF)));
            return opcode + rs + rt + offsetBinary;
        }
    }

    private static String processJump(String[] parts, String op, Map<String, Integer> labelMap) {
        String opcode = opcodeMap.get(op);
        String label = parts[1];
        int targetAddress = labelMap.get(label);
        int address = targetAddress / 4;
        String addressBinary = String.format("%026d", Integer.parseInt(Integer.toBinaryString(address)));
        return opcode + addressBinary;
    }

    private static String getRegister(String reg) {
        return registerMap.getOrDefault(reg.trim().toLowerCase(), "00000");
    }

    private static String binaryToHex(String binary) {
        if (binary.length() != 32 && !binary.startsWith("Invalid")) {
            return "Invalid binary length";
        }

        if (binary.startsWith("Invalid")) {
            return binary;
        }

        long decimal = Long.parseLong(binary, 2);
        return "0x" + String.format("%08X", decimal);
    }
}