package io.wattom.launcher;

import com.sun.tools.attach.VirtualMachine;

public final class AttachHelper {
    private AttachHelper() {}

    public static void attach(String pid, String agentJar, String agentArgs) throws Exception {
        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            vm.loadAgent(agentJar, agentArgs);
        } finally {
            vm.detach();
        }
    }
}
