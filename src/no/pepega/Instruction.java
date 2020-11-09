package no.pepega;

public class Instruction {
    enum Inst {
        Add,
        Addw,
        Addi,
        Addiw,
        And,
        Andi,
        Auipc,
        Beq,
        Bge,
        Bgeu,
        Blt,
        Bltu,
        Bne,
        Csrrc,
        Csrrci,
        Csrrs,
        Csrrsi,
        Csrrw,
        Csrrwi,
        Ebreak,
        Ecall,
        Fence,
        FenceI,
        Jal,
        Jalr,
        Lb,
        Lbu,
        Lh,
        Lhu,
        Lw,
        Lwu,
        Ld,
        Ldu,
        Lui,
        Mul,
        Mulh,
        Mulhsu,
        Mulhu,
        Div,
        Divu,
        Rem,
        Remu,
        Or,
        Ori,
        RdCycle,
        RdCycleH,
        RdTime,
        RdTimeH,
        RdInstRet,
        RdInstRetH,
        Sb,
        Sh,
        Sw,
        Sd,
        Sll,
        Sllw,
        Slli,
        Slliw,
        Slt,
        Slti,
        Sltu,
        Sltiu,
        Sra,
        Sraw,
        Srai,
        Sraiw,
        Srl,
        Srlw,
        Srli,
        Srliw,
        Sub,
        Subw,
        Xor,
        Xori,
    }

    public int rs2;
    public int rs1;
    public int rd;
    public int funct3;
    public int funct7;
    public long csr;
    public int opcode;
    public Inst op;
    public long shamt;
    public int immediate;
    public byte fence;

    public static String regName(int reg) {
        switch (reg) {
            case 0: return "zero";
            case 1: return "ra";
            case 2: return "sp";
            case 3: return "gp";
            case 4: return "tp";
            case 5: return "t0";
            case 6: return "t1";
            case 7: return "t2";
            case 8: return "s0";
            case 9: return "s1";
            case 10: return "a0";
            case 11: return "a1";
            case 12: return "a2";
            case 13: return "a3";
            case 14: return "a4";
            case 15: return "a5";
            case 16: return "a6";
            case 17: return "a7";
            case 18: return "s2";
            case 19: return "s3";
            case 20: return "s4";
            case 21: return "s5";
            case 22: return "s6";
            case 23: return "s7";
            case 24: return "s8";
            case 25: return "s9";
            case 26: return "s10";
            case 27: return "s11";
            case 28: return "t3";
            case 29: return "t4";
            case 30: return "t5";
            case 31: return "t6";
            default: return "Unknown register";
        }
    }

    public static void printInfo(Instruction inst, long i, int opcode) {
        if (inst == null) {
            System.out.printf("0x%02X: Unknown instruction\n", i);
            return;
        }
        System.out.printf("0x%02X: %08x\t", i, opcode);
        System.out.print(inst.op+"  \t\t\t\t");
        String rs1 = Instruction.regName(inst.rs1);
        String rs2 = Instruction.regName(inst.rs2);
        String rd = Instruction.regName(inst.rd);
        switch (inst.op) {
            case Add: case And: case Addw: {
                System.out.printf("%s, %s, %s", rd, rs1, rs2);
                break;
            }
            case Addi: case Addiw: case Andi: {
                System.out.printf("%s, %s, %d", rd, rs1, inst.immediate);
                break;
            }
            case Beq: case Bne: case Blt: case Bge: case Bltu: case Bgeu: {
                System.out.printf("%s, %s, %d\t\t# 0x%02X", rd, rs1, inst.immediate, i+inst.immediate);
                break;
            }
            case Jal: {
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
    }

    public static Instruction decode(int inst) {
        int opcode = inst&0x7F;
        int rd = (inst>>>7)&0x1f;
        int rs1 = (inst>>>15)&0x1f;
        int rs2 = (inst>>>20)&0x1f;

        int funct3 = (inst>>>12)&0x7;
        int funct7 = (inst>>>25)&0x7f;

        Instruction DecodedInst = new Instruction();
        DecodedInst.opcode = opcode;
        DecodedInst.rd = rd;
        DecodedInst.rs1 = rs1;
        DecodedInst.rs2 = rs2;
        DecodedInst.funct3 = funct3;
        DecodedInst.funct7 = funct7;

        switch (opcode) {
            case 0x03: /* Loads */ {
                DecodedInst.immediate = inst >> 20;
                if (funct3 == 0b000) { /* LB */
                    DecodedInst.op = Instruction.Inst.Lb;
                } else if (funct3 == 0b001) { /* LH */
                    DecodedInst.op = Instruction.Inst.Lh;
                } else if (funct3 == 0b010) { /* LW */
                    DecodedInst.op = Instruction.Inst.Lw;
                } else if (funct3 == 0b011) { /* LD */
                    DecodedInst.op = Instruction.Inst.Ld;
                } else if (funct3 == 0b110) { /* LWU */
                    DecodedInst.op = Instruction.Inst.Lwu;
                } else {
                    System.err.println("Illegal instruction");
                    return null;
                }
                break;
            }
            case 0x33: /* reg math */ {
                if (funct3 == 0x0) { /* ADD */
                    if (((inst >> 30) & 0x1) == 1)
                        DecodedInst.op = Instruction.Inst.Sub;
                    else
                        DecodedInst.op = Instruction.Inst.Add;
                } else if (funct3 == 0x2) { /* SLT */
                    DecodedInst.op = Instruction.Inst.Slt;
                } else if (funct3 == 0x3) { /* SLTU */
                    DecodedInst.op = Instruction.Inst.Sltu;
                } else if (funct3 == 0x4) { /* XOR */
                    DecodedInst.op = Instruction.Inst.Xor;
                } else if (funct3 == 0x6) { /* OR */
                    DecodedInst.op = Instruction.Inst.Or;
                } else if (funct3 == 0x7) { /* AND */
                    DecodedInst.op = Instruction.Inst.And;
                } else if (funct3 == 0x1) { /* SLL */
                    DecodedInst.op = Instruction.Inst.Sll;
                    DecodedInst.shamt = DecodedInst.immediate & 0x3f;
                } else { /* SRL/SRA */
                    DecodedInst.shamt = DecodedInst.immediate & 0x3f;
                    if (((inst >>> 30) & 0x1) == 0)
                        DecodedInst.op = Instruction.Inst.Srl;
                    else
                        DecodedInst.op = Instruction.Inst.Sra;
                }
                break;
            }
            case 0x13: /* immediate math */ {
                DecodedInst.immediate = (inst&0xfff00000)>>20;

                if (funct3 == 0x0) { /* ADDI */
                    DecodedInst.op = Instruction.Inst.Addi;
                } else if (funct3 == 0x2) { /* SLTI */
                    DecodedInst.op = Instruction.Inst.Slti;
                } else if (funct3 == 0x3) { /* SLTIU */
                    DecodedInst.op = Instruction.Inst.Sltiu;
                } else if (funct3 == 0x4) { /* XORI */
                    DecodedInst.op = Instruction.Inst.Xori;
                } else if (funct3 == 0x6) { /* ORI */
                    DecodedInst.op = Instruction.Inst.Ori;
                } else if (funct3 == 0x7) { /* ANDI */
                    DecodedInst.op = Instruction.Inst.Andi;
                } else if (funct3 == 0x1) { /* SLLI */
                    DecodedInst.op = Instruction.Inst.Slli;
                    DecodedInst.shamt = DecodedInst.immediate & 0x3f;
                } else { /* SRLI/SRAI */
                    DecodedInst.shamt = DecodedInst.immediate & 0x3f;
                    if (((inst >>> 30) & 0x1) == 0)
                        DecodedInst.op = Instruction.Inst.Srli;
                    else
                        DecodedInst.op = Instruction.Inst.Srai;
                }
                break;
            }
            case 0x17: /* AUIPC */ {
                DecodedInst.op = Instruction.Inst.Auipc;
                break;
            }
            case 0x3B: /* 32-Bit register math */ {
                if (funct3 == 0x0) { /* ADD */
                    if (((inst >> 30) & 0x1) == 1)
                        DecodedInst.op = Instruction.Inst.Subw;
                    else
                        DecodedInst.op = Instruction.Inst.Addw;
                } else if (funct3 == 0x1) { /* SLLW */
                    DecodedInst.op = Instruction.Inst.Sllw;
                    DecodedInst.shamt = DecodedInst.immediate & 0x3f;
                } else if (funct3 == 0x5) { /* SRLW/SRAW */
                    DecodedInst.shamt = DecodedInst.immediate & 0x3f;
                    if (((inst >>> 30) & 0x1) == 0)
                        DecodedInst.op = Instruction.Inst.Srlw;
                    else
                        DecodedInst.op = Instruction.Inst.Sraw;
                } else {
                    System.err.printf("Unknown 32-Bit register math op funct3: 0x%02X%n", funct3);
                    return null;
                }
                break;
            }
            case 0x1B: /* 32-Bit immediate math */ {
                DecodedInst.immediate = (inst&0xfff00000)>>20;
                if (funct3 == 0b000) { /* ADDIW */
                    DecodedInst.op = Instruction.Inst.Addiw;
                } else if (funct3 == 0b001) { /* SLLIW */
                    DecodedInst.op = Instruction.Inst.Slliw;
                } else if (funct3 == 0b101) { /* SRLIW/SRAIW */
                    if (((inst >>> 30) & 1) == 0)
                        DecodedInst.op = Instruction.Inst.Srliw;
                    else
                        DecodedInst.op = Instruction.Inst.Sraiw;
                } else {
                    System.err.printf("Unknown 32-Bit immediate math op funct3: 0x%02X%n", funct3);
                    return null;
                }
                break;
            }
            case 0x23: /* Saves */ {
                DecodedInst.immediate = (inst&0xfe000000)>>20 | ((inst >>> 7) & 0x1f);
                if (funct3 == 0b000) { /* SB */
                    DecodedInst.op = Instruction.Inst.Sb;
                } else if (funct3 == 0b001) { /* SH */
                    DecodedInst.op = Instruction.Inst.Sh;
                } else if (funct3 == 0b010) { /* SW */
                    DecodedInst.op = Instruction.Inst.Sw;
                } else if (funct3 == 0b011) { /* SD */
                    DecodedInst.op = Instruction.Inst.Sd;
                } else {
                    System.err.printf("Unknown save funct3: 0x%02X", funct3);
                    return null;
                }
                break;
            }
            case 0x37: /* LUI */ {
                DecodedInst.op = Instruction.Inst.Lui;
                break;
            }
            case 0x63: /* Conditional jumps */ {
                DecodedInst.immediate = (inst&0x80000000)>>19 | (inst<<4)&0x800 | (inst>>>20)&0x7e0 | (inst>>>7)&0x1e;

                switch (funct3) {
                    case 0x0: {
                        DecodedInst.op = Instruction.Inst.Beq;
                        break;
                    }
                    case 0x1: {
                        DecodedInst.op = Instruction.Inst.Bne;
                        break;
                    }
                    case 0x4: {
                        DecodedInst.op = Instruction.Inst.Blt;
                        break;
                    }
                    case 0x5: {
                        DecodedInst.op = Instruction.Inst.Bge;
                        break;
                    }
                    case 0x6: {
                        DecodedInst.op = Instruction.Inst.Bltu;
                        break;
                    }
                    case 0x7: {
                        DecodedInst.op = Instruction.Inst.Bgeu;
                        break;
                    }
                }
                break;
            }
            case 0x6f: /* JAL */ {
                DecodedInst.immediate = ((inst&0x80000000)>>11) + (inst&0xFF000) + ((inst>>>9)&0x800) + ((inst>>>20)&0x7FE);
                DecodedInst.op = Instruction.Inst.Jal;
                break;
            }
            case 0x67: /* JALR */ {
                DecodedInst.immediate = (inst&0xFFF00000)>>20;
                DecodedInst.op = Instruction.Inst.Jalr;
                break;
            }
            default: {
                System.err.printf("Unknown opcode: 0x%02X\n", opcode);
                return null;
            }
        }
        return DecodedInst;
    }

}
