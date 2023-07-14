package com.xunmeng.pinduoduo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.StringObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.jni.ProxyClassFactory;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AntiToken {

    private final AndroidEmulator emulator;

    private final DvmClass cDeviceNative;
    private final VM vm;

    public AntiToken() {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.xunmeng.pinduoduo")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM();
        vm.setDvmClassFactory(new ProxyClassFactory());
        vm.setVerbose(false);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/armeabi-v7a/libPddSecure.so"), false);
        cDeviceNative = vm.resolveClass("com/xunmeng/pinduoduo/secure/DeviceNative");
        dm.callJNI_OnLoad(emulator);
    }

    public void destroy() throws IOException {
        emulator.close();
    }

    public String info2() {
        String methodSign = "info2(Landroid/content/Context;J)Ljava/lang/String;";
        RegisterContext context = emulator.getContext();
        long j = 1688018564388L;
        StringObject obj = cDeviceNative.callStaticJniMethodObject(emulator, methodSign, context, j);
        return obj.getValue();
    }


    public static void main(String[] args) throws Exception {
        AntiToken antiToken = new AntiToken();
        String anti_token = antiToken.info2();
        System.out.println("anti_token=" + anti_token);
        antiToken.destroy();
    }
}
