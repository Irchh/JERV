package no.pepega;

import java.util.Arrays;
import java.util.Map;

/** The VM itself. This is just an example, it's not a "real" interface. */
public class PseudoVM {
    public boolean allowDynamicResize = false;
    Machine machine;

    String addr_comp = null;
    String addr_gpu = null;
    String addr_screen = null;
    String addr_eeprom = null;

    boolean is_booted = false;
    RISCV arch;

    public PseudoVM(Machine machine) {
        this.machine = machine;
        this.arch = new RISCV(this, machine, 4096*1024);
    }

    void bsod(String msg) {
        if(this.arch.hard_halted)
            return;

        System.err.println(msg);
        System.err.printf("pc = %08X, op = %08X\n"
                , arch.pc, (short)(arch.memory[(int)arch.pc]&0x7F));
        try {
            if (addr_gpu != null) {
                machine.invoke(addr_gpu, "setForeground", new Object[]{0xFFFFFF, false});
                Object[] gpuDepthO = machine.invoke(addr_gpu, "getDepth", new Object[]{});
                int depth = 1;
                if (gpuDepthO != null && gpuDepthO.length >= 1 && gpuDepthO[0] instanceof Integer)
                    depth = (Integer) gpuDepthO[0];
                machine.invoke(addr_gpu, "setBackground", new Object[]{0x0000FF, false});
                Object[] gpuSizeO = machine.invoke(addr_gpu, "getResolution",
                        new Object[]{});
                int w = 40;
                int h = 16;
                if (gpuSizeO != null && gpuSizeO.length >= 1 && gpuSizeO[0] instanceof Integer)
                    w = (Integer) gpuSizeO[0];
                if (gpuSizeO != null && gpuSizeO.length >= 2 && gpuSizeO[1] instanceof Integer)
                    h = (Integer) gpuSizeO[1];
                machine.invoke(addr_gpu, "fill", new Object[]{1, 1, w, h, " "});
                machine.invoke(addr_gpu, "set", new Object[]{w / 2 + 1 - 6, h / 2 - 1, "FATAL ERROR:"});
                machine.invoke(addr_gpu, "set", new Object[]{w / 2 + 1 - msg.length() / 2, h / 2 + 1, msg});
            }
        } catch (Exception e) {
            System.err.printf("exception fired in BSOD message - ignored\n");
            e.printStackTrace();
        }
        this.arch.hard_halted = true;
    }

    Object run(int mode) throws Exception {
        if (!is_booted) {
            if (mode != 0) {
                System.out.printf("Waiting for sync call...\n");
                return null;
            }
            machine.beep((short)400, (short)20);
            System.out.printf("Booting!\n");
            System.out.printf("Components: %d\n", this.machine.componentCount());

            this.is_booted = true;
            Map<String, String> m = this.machine.components();
            for(String k: m.keySet()) {
                String v = m.get(k);
                if(v.equals("computer")) this.addr_comp = k;
                if(v.equals("gpu")) this.addr_gpu = k;
                if(v.equals("screen")) this.addr_screen = k;
                if(v.equals("eeprom")) this.addr_eeprom = k;
                System.out.printf(" - %s = %s\n", k, v);
            }

            if (addr_screen != null && addr_gpu != null) {
                machine.invoke(addr_gpu, "bind", new Object[]{addr_screen});
            }

            if (addr_gpu != null) {
                machine.invoke(addr_gpu, "setForeground", new Object[]{0xFFFFFF, false});
                machine.invoke(addr_gpu, "setBackground", new Object[]{0x000000, false});
                Object[] gpuSizeO = machine.invoke(addr_gpu, "getResolution", new Object[]{});
                int w = 40;
                int h = 16;
                if(gpuSizeO.length >= 1 && gpuSizeO[0] instanceof Integer)
                    w = (Integer)gpuSizeO[0];
                if(gpuSizeO.length >= 2 && gpuSizeO[1] instanceof Integer)
                    h = (Integer)gpuSizeO[1];
                machine.invoke(addr_gpu, "fill", new Object[]{1, 1, w, h, " "});
                machine.invoke(addr_gpu, "set", new Object[]{w/2, h/2, ":-)"});
            }

            // Clear RAM
            Arrays.fill(arch.memory, (byte) 0);

            // Setup stack pointer
            arch.regs[2] = arch.mem_size;
            System.err.printf("Stack pointer: 0x%X", arch.regs[2]);

            // Load EEPROM
            if(addr_eeprom != null) {
                Object[] eeDataO = machine.invoke(addr_eeprom, "get", new Object[]{});
                if (eeDataO != null && eeDataO.length >= 1 && eeDataO[0] instanceof byte[]) {
                    byte[] eeprom_data = (byte[])eeDataO[0];
                    try {
                        for(int i = 0; i < eeprom_data.length; i++) {
                            arch.mem_write_8(arch.progLoad()+i, eeprom_data[i]);
                        }

                        System.err.printf("BIOS loaded - EEPROM size = %08X, pc = %08X\n"
                                , eeprom_data.length, arch.pc);
                    } catch(Exception e) {
                        System.err.printf("exception fired in EEPROM load\n");
                        e.printStackTrace();
                        bsod("Exception loading EEPROM!");
                    }
                }
            } else {
                bsod("No EEPROM found!");
            }
        }

        // Run some cycles
        if(mode == 1) { mode = 2; } // TEST

        try {
            if(mode == 1) return (Integer)0;

            if(!arch.hard_halted) {
                // TODO: adaptive cycle count
                int cycs = (mode == 0 ? 200 : 40*1000*1000/20);
                arch.run_cycles(cycs);
                if(arch.hard_halted){
                    bsod("Halted");
                } else if(arch.sync_call) {
                    return null;
                } else if(arch.need_sleep) {
                    return (Integer)1;
                }
            }
        } catch (Exception e) {
            bsod("CPU crashed");
            System.err.printf("CPU crashed:\n");
            e.printStackTrace();
        }

        return (Integer)0;
    }
}
