package com.tesla.easyso1;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.Module;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.Dobby;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;

/**
 * <a href="https://bbs.pediy.com/thread-263345.htm">...</a>
 * 成功模拟!
 */
public class MainActivity extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private DvmClass dvmClass;
    private String className = "com/tesla/easyso1/MainActivity";
    private Module module;

    private MainActivity() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
//                .addBackendFactory(new DynarmicFactory(true))
                .build();
        Memory memory = emulator.getMemory();
        LibraryResolver resolver = new AndroidResolver(23);
        memory.setLibraryResolver(resolver);

        vm = emulator.createDalvikVM((File) null);
        vm.setVerbose(false);
        vm.setJni(this);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/armeabi-v7a/libeasyso1.so"), false);

        // hook
        module = dm.getModule();
        Dobby dobby = Dobby.getInstance(emulator);
        // 2. 使用ida pro查看导出方法名，尝试hook
        dobby.replace(module.findSymbolByName("_Z24function_check_tracerPIDv"), new ReplaceCallback() { // 使用Dobby inline hook导出函数
            @Override
            // 3. contextk可以拿到参数，originFunction是原方法的地址
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                System.out.println("_Z24function_check_tracerPIDv.onCall function address => 0x" + Long.toHexString(originFunction));
//                return HookStatus.RET(emulator, originFunction);
//                return null;
                return HookStatus.LR(emulator, 0);
            }

            @Override

            public void postCall(Emulator<?> emulator, HookContext context) {
                System.out.println(" calling _Z24function_check_tracerPIDv .... return false");
//                context.getIntArg(0) ;
            }
        }, false);

        dm.callJNI_OnLoad(emulator);
        dvmClass = vm.resolveClass(className);
    }

    private void crack() {
        long start = System.currentTimeMillis();
        DvmObject<?> result = dvmClass.callStaticJniMethodObject(emulator, "sign(Ljava/lang/String;)Ljava/lang/String;", "123456");
        System.out.println("final result is => " + result.getValue());
        System.out.println((System.currentTimeMillis() - start) + "ms");
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        MainActivity mainActivity = new MainActivity();
        System.out.println("load offset=" + (System.currentTimeMillis() - start) + "ms");
        mainActivity.crack();
    }
}
