package no.pepega;

import java.io.File;
import java.nio.file.Files;

public class Main {

    public static void main(String[] args) throws Exception {
        File file = new File("./res/eeprom.ch8");

        byte[] eeprom = Files.readAllBytes(file.toPath());

        for (int i = 0; i < eeprom.length; i += 4) {
            int opcode = Byte.toUnsignedInt(eeprom[i])|Byte.toUnsignedInt(eeprom[i+1])<<8|Byte.toUnsignedInt(eeprom[i+2])<<16|Byte.toUnsignedInt(eeprom[i+3])<<24;
            Instruction inst = Instruction.decode(opcode);
            if (inst == null) {
                System.out.printf("0x%02X: Unknown instruction\n", i);
                continue;
            }
            System.out.printf("0x%02X: %08x\t", i, opcode);
            System.out.print(inst.op+" ");
            String rs1 = Instruction.regName(inst.rs1);
            String rs2 = Instruction.regName(inst.rs2);
            String rd = Instruction.regName(inst.rd);
            switch (inst.op) {
                case Add: case And: case Addw: {
                    System.out.printf("%s, %s, %s", rd, rs1, rs2);
                    break;
                }
                case Addi: case Addiw: case Andi: case Jalr: {
                    System.out.printf("%s, %s, %d", rd, rs1, inst.immediate);
                    break;
                }
                case Beq: case Bne: case Blt: case Bge: case Bltu: case Bgeu: {
                    System.out.printf("%s, %s, %d\t\t# 0x%02X", rs1, rs2, inst.immediate, i+inst.immediate);
                    break;
                }
                case Jal: case Auipc: {
                    System.out.printf("%s, %d\t\t# 0x%02X", rd, inst.immediate, i+inst.immediate);
                    break;
                }
                case Lb: case Lh: case Lw: case Ld: {
                    System.out.printf("%s, %d(%s)", rd, inst.immediate, rs1);
                    break;
                }
                case Sb: case Sh: case Sw: case Sd: {
                    System.out.printf("%s, %d(%s)", rs2, inst.immediate, rs1);
                    break;
                }
            }
            System.out.println();
        }

        Machine machine = new Machine(eeprom);
        PseudoVM vm = new PseudoVM(machine);
        vm.run(0);
        vm.arch.step = true;
        while (!vm.arch.hard_halted && vm.is_booted) {
            vm.run(1);
        }
	    System.out.println("Hello, World!");
    }
}
