package no.pepega;

import java.io.IOException;
import java.util.Scanner;

public class RISCV {
    Machine machine;
    PseudoVM vm;

    long[] regs;
    long pc;
    byte[] memory;
    int mem_size;

    public boolean step = false;
    public boolean hard_halted;
    boolean need_sleep;
    boolean sync_call;

    private int cycles;
    private int cycle_wait;

    public RISCV (PseudoVM vm, Machine machine, int mem_size) {
        this.machine = machine;
        this.vm = vm;

        this.regs = new long[32];
        // Set SP to the end of memory (x2 is SP)
        this.regs[2] = mem_size;
        this.pc = 0;

        this.memory = new byte[mem_size];
        this.mem_size = mem_size;

        this.hard_halted = false;
        this.need_sleep = false;
        this.sync_call = false;

        this.cycles = 0;
        this.cycle_wait = 0;
    }

    public void execute(Instruction i) {

        if (i == null) {
            dumpRegs();
            panic("Unknown instruction");
            return;
        }

        regs[0] = 0;

        switch (i.op) {
            case Add: {
                regs[i.rd] = regs[i.rs1] + regs[i.rs2];
                //System.out.printf("0x%02X: Add x%d, x%d, x%d\n", pc, i.rd, i.rs1, i.rs2);
                break;
            }
            case Addi: {
                regs[i.rd] = regs[i.rs1] + i.immediate;
                //System.out.printf("0x%02X: Addi x%d, x%d, %d\n", pc, i.rd, i.rs1, i.immediate);
                break;
            }
            case Addiw: {
                //System.out.printf("x%d = x%d[%d]+%d = ", i.rd, i.rs1, regs[i.rs1], i.immediate);
                regs[i.rd] = Integer.toUnsignedLong((int) (regs[i.rs1] + i.immediate));
                //System.out.printf("%d\n", regs[i.rd]);
                if (((regs[i.rd]>>>31)&1) != 0) {
                    regs[i.rd] |= Integer.toUnsignedLong(0xFFFFFFFF)<<32;
                }else{
                    regs[i.rd] &= 0xFFFFFFFF;
                }
                //System.out.printf("0x%02X: Addiw x%d, x%d, %d\n", pc, i.rd, i.rs1, i.immediate);
                break;
            }
            case And: {
                regs[i.rd] = regs[i.rs1] & regs[i.rs2];
                //System.out.printf("0x%02X: And x%d, x%d, x%d\n", pc, i.rd, i.rs1, i.rs2);
                break;
            }
            case Andi: {
                regs[i.rd] = regs[i.rs1] & i.immediate;
                //System.out.printf("0x%02X: And x%d, x%d, %d\n", pc, i.rd, i.rs1, i.immediate);
                break;
            }
            case Auipc: {
                regs[i.rd] = i.opcode&0xFFFFF000;
                if ((i.opcode&0x80000000) != 0)
                    regs[i.rd] |= (long)0xFFFFFFFF<<32;
                regs[i.rd] += pc;
                break;
            }
            case Beq: {
                //System.out.printf("0x%02X: Beq x%d, x%d, %d # 0x%02X\n", pc, i.rs1, i.rs2, i.immediate, pc+i.immediate);
                if (regs[i.rs1] == regs[i.rs2]) {
                    //System.out.println("Beq Jumped");
                    pc += i.immediate-4;
                }
                break;
            }
            case Bne: {
                //System.out.printf("0x%02X: Bne x%d, x%d, %d # 0x%02X\n", pc, i.rs1, i.rs2, i.immediate, pc+i.immediate);
                if (regs[i.rs1] != regs[i.rs2]) {
                    //System.out.println("Bne Jumped");
                    pc += i.immediate-4;
                }
                break;
            }
            case Blt: {
                //System.out.printf("0x%02X: Blt x%d, x%d, %d\n", pc, i.rs1, i.rs2, i.immediate);
                if (regs[i.rs1] < regs[i.rs2]) {
                    //System.out.println("Blt Jumped");
                    pc += i.immediate-4;
                }
                break;
            }
            case Bge: {
                //System.out.printf("0x%02X: Bge x%d, x%d, %d\n", pc, i.rs1, i.rs2, i.immediate);
                if (regs[i.rs1] >= regs[i.rs2]) {
                    //System.out.println("Bge Jumped");
                    pc += i.immediate-4;
                }
                break;
            }
            case Bltu: {
                //System.out.printf("0x%02X: Bltu x%d, x%d, %d\n", pc, i.rs1, i.rs2, i.immediate);
                if (Long.compareUnsigned(regs[i.rs1], regs[i.rs2]) < 0) {
                    //System.out.println("Bltu Jumped");
                    pc += i.immediate-4;
                }
                break;
            }
            case Bgeu: {
                //System.out.printf("0x%02X: Bgeu x%d, x%d, %d\n", pc, i.rs1, i.rs2, i.immediate);
                if (Long.compareUnsigned(regs[i.rs1], regs[i.rs2]) >= 0) {
                    //System.out.println("Bgeu Jumped");
                    pc += i.immediate-4;
                }
                break;
            }
            case Jal: {
                if (((i.opcode>>31)&0x1) != 0)
                    i.immediate |= (long)0xFFFFFFFF<<32;
                //System.out.printf("0x%02X: Jal x%d, %d\n", pc, i.rd, i.immediate);
                regs[i.rd] = pc+4;
                pc += i.immediate-4;
                break;
            }
            case Jalr: {
                //System.out.printf("0x%02X: Jalr x%d, %d\n", pc, i.rd, i.immediate);
                regs[i.rd] = pc+i.immediate;
                //pc += ((regs[i.rs1]+i.immediate)&(~(long)1))-4;
                break;
            }
            case Sb: {
                write8((int)(regs[i.rs1]+i.immediate), (byte)regs[i.rs2]);
                //System.out.printf("0x%02X: Sb x%d, %d(x%d)\n", pc, i.rs2, i.immediate, i.rs1);
                break;
            }
            case Sh: {
                write16((int)(regs[i.rs1]+i.immediate), (short)regs[i.rs2]);
                //System.out.printf("0x%02X: Sh x%d, %d(x%d)\n", pc, i.rs2, i.immediate, i.rs1);
                break;
            }
            case Sw: {
                write32((int)(regs[i.rs1]+i.immediate), (int)regs[i.rs2]);
                //System.out.printf("0x%02X: Sw x%d, %d(x%d)\n", pc, i.rs2, i.immediate, i.rs1);
                break;
            }
            case Sd: {
                write64((int)(regs[i.rs1]+i.immediate), (long)regs[i.rs2]);
                //System.out.printf("0x%02X: %08x Sd x%d, %d(x%d)\n", pc, inst, i.rs2, i.immediate, i.rs1);
                break;
            }
            case Lb: /* LB */ {
                regs[i.rd] = memory[(int)(regs[i.rs1]+i.immediate)];
                //System.out.printf("0x%02X: Lb x%d, %d(x%d)\n", pc, i.rd, i.immediate, i.rs1);
                break;
            }
            case Lh: /* LH */ {
                regs[i.rd] = read16((int)(regs[i.rs1]+i.immediate));
                //System.out.printf("0x%02X: Lh x%d, %d(x%d)\n", pc, i.rd, i.immediate, i.rs1);
                break;
            }
            case Lw: /* LW */ {
                regs[i.rd] = read32((int) (regs[i.rs1]+i.immediate));
                //System.out.printf("0x%02X: Lw x%d, %d(x%d)\n", pc, i.rd, i.immediate, i.rs1);
                break;
            }
            case Ld: /* LD */ {
                regs[i.rd] = read64((int)(regs[i.rs1]+i.immediate));
                //System.out.printf("0x%02X: Ld x%d, %d(x%d)\n", pc, i.rd, i.immediate, i.rs1);
                break;
            }
            case Lwu: /* LWU */ {
                regs[i.rd] = Integer.toUnsignedLong(read32((int)(regs[i.rs1]+i.immediate)));
                System.out.printf("0x%02X: Lwu x%d, %d(x%d)\n", pc, i.rd, i.immediate, i.rs1);
                break;
            }
            default: {
                panic("Unknown instruction");
                this.dumpRegs();
            }
        }

        this.pc += 4;
        if (this.pc >= this.mem_size || this.pc == 0)
            vm.bsod("Program counter out of range!");
    }

    private void panic(String str) {
        vm.bsod(str);
    }

    private void dumpRegs() {
        String output = new String("");
        for (int i = 0; i < 32; i += 4) {
            output = String.format(
                    "%s\n%s",
                    output,
                    String.format(
                            "x%01d=0x%X x%01d=0x%X x%01d=0x%X x%01d=0x%X ",
                            i, regs[i],
                            i + 1, regs[i + 1],
                            i + 2, regs[i + 2],
                            i + 3, regs[i + 3]
                    )
            );
        }
        System.out.println(output);
    }

    public int fetch() {
        return read32((int)pc);
    }

    public int progLoad() {
        return 0x00;
    }

    private short read16(int addr) {
        return (short)((memory[addr]) | memory[(addr+1)]<<8);
    }

    private int read32(int addr) {
        return Byte.toUnsignedInt(memory[addr]) | Byte.toUnsignedInt(memory[(addr+1)])<<8 | Byte.toUnsignedInt(memory[(addr+2)])<<16 | Byte.toUnsignedInt(memory[(addr+3)])<<24;
    }

    private long read64(int addr) {
        return Integer.toUnsignedLong(read32(addr)) | Integer.toUnsignedLong(read32(addr+4))<<32;
    }

    private void write8(int addr, byte data) {
        memory[addr] = data;
    }

    private void write16(int addr, short data) {
        write8(addr, (byte)(data));
        write8(addr+1, (byte)(data>>>8));
    }

    private void write32(int addr, int data) {
        write16(addr, (short)(data));
        write16(addr+1, (short)(data>>>16));
    }

    private void write64(int addr, long data) {
        write32(addr, (int)(data));
        write32(addr+1, (int)(data>>>32));
    }

    public void mem_write_8(int addr, byte data) {
        memory[addr] = data;
    }

    public void run_cycles(int ccount) {
        assert(ccount > 0);
        assert(ccount < 0x40000000);

        int cyc_end = this.cycles + ccount - this.cycle_wait;

        this.need_sleep = false;
        while(!this.hard_halted && !this.need_sleep && (cyc_end - this.cycles) >= 0) {
            long pc = this.pc;

            try {
                int inst = fetch();
                Instruction i = Instruction.decode(inst);
                //long oldpc = this.pc;
                //this.dumpRegs();
                this.execute(i);
                //Instruction.printInfo(i, oldpc, inst);
                //this.dumpRegs();
                //new Scanner(System.in).nextLine();
            } catch (RuntimeException e) {
                System.err.printf("Exception in MIPS emu - halted!\n");
                this.vm.bsod(e.getMessage());
                e.printStackTrace();
                this.hard_halted = true;
            }
        }

        this.cycle_wait = Math.max(0, this.cycles - cyc_end);
    }
}
