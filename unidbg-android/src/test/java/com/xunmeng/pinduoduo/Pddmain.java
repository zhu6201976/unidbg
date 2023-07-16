package com.xunmeng.pinduoduo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidARMEmulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Pddmain extends AbstractJni implements IOResolver<AndroidFileIO> {

    private AndroidEmulator androidEmulator;
    private static final String APK_PATH = "/Users/Downloads/com.xunmeng.pinduoduo_6.7.0_60700.apk";
    private static final String SO_PATH = "/Users/Downloads/com.xunmeng.pinduoduo_6.7.0_60700/lib/armeabi-v7a/libpdd_secure.so";
    private Module moduleModule;
    private VM dalvikVM;

    public static void main(String[] args) {
        Pddmain main = new Pddmain();
        main.create();
    }

    private void create() {
        AndroidEmulatorBuilder androidEmulatorBuilder = new AndroidEmulatorBuilder(false) {
            @Override
            public AndroidEmulator build() {
                return new AndroidARMEmulator("com.xunmeng.pinduoduo",rootDir,backendFactories) {
//                    @Override
//                    protected UnixSyscallHandler<AndroidFileIO> createSyscallHandler(SvcMemory svcMemory) {
//                        return new PddArmSysCallHand(svcMemory);
//                    }
                };
            }
        };
        androidEmulator = androidEmulatorBuilder.setProcessName("").build();
        androidEmulator.getSyscallHandler().addIOResolver(this);
        Memory androidEmulatorMemory = androidEmulator.getMemory();
        androidEmulatorMemory.setLibraryResolver(new AndroidResolver(23));
        dalvikVM = androidEmulator.createDalvikVM(new File(APK_PATH));
        DalvikModule module = dalvikVM.loadLibrary(new File(SO_PATH), true);
        moduleModule = module.getModule();
        dalvikVM.setJni(this);
        dalvikVM.setVerbose(true);
        dalvikVM.callJNI_OnLoad(androidEmulator, moduleModule);
        callInfo3();
    }

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("com/tencent/mars/xlog/PLog->i(Ljava/lang/String;Ljava/lang/String;)V".equals(signature)) {
            return;
        }
        super.callStaticVoidMethodV(vm, dvmClass, signature, vaList);
    }

    private void callInfo3() {
        List<Object> argList = new ArrayList<>();
        argList.add(dalvikVM.getJNIEnv());
        argList.add(0);
        DvmObject<?> context = dalvikVM.resolveClass("android/content/Context").newObject(null);
        argList.add(dalvikVM.addLocalObject(context));
        argList.add(dalvikVM.addLocalObject(new StringObject(dalvikVM, "api/oak/integration/render")));
        argList.add(dalvikVM.addLocalObject(new StringObject(dalvikVM, "dIrjGpkC")));
//        Number number = moduleModule.callFunction(androidEmulator, 0xb6f9, argList.toArray())[0];
//        String toString = dalvikVM.getObject(number.intValue()).getValue().toString();
//        System.out.println(toString);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("com/xunmeng/pinduoduo/secure/EU->gad()Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm, "cb14a9e76b72a627");
        } else if ("java/util/UUID->randomUUID()Ljava/util/UUID;".equals(signature)) {
            UUID uuid = UUID.randomUUID();
            DvmObject<?> dvmObject = vm.resolveClass("java/util/UUID").newObject(uuid);
            return dvmObject;
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if ("java/util/UUID->toString()Ljava/lang/String;".equals(signature)) {
            UUID uuid = (UUID) dvmObject.getValue();
            return new StringObject(vm, uuid.toString());
        } else if ("java/lang/String->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;".equals(signature)) {
            String obj = dvmObject.getValue().toString();
            String arg0 = vaList.getObjectArg(0).toString();
            String arg1 = vaList.getObjectArg(1).toString();
            String replaceAll = obj.replaceAll(arg0, arg1);
            return new StringObject(vm, replaceAll);

        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        if ("java/lang/String->hashCode()I".equals(signature)) {
            return dvmObject.getValue().toString().hashCode();
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        if ("/proc/stat".equals(pathname)) {
            String info = "cpu  15884810 499865 12934024 24971554 59427 3231204 945931 0 0 0\n" +
                    "cpu0 6702550 170428 5497985 19277857 45380 1821584 529454 0 0 0\n" +
                    "cpu1 4438333 121907 3285784 1799772 3702 504395 255852 0 0 0\n" +
                    "cpu2 2735453 133666 2450712 1812564 4626 538114 93763 0 0 0\n" +
                    "cpu3 2008473 73862 1699542 2081360 5716 367109 66860 0 0 0\n" +
                    "intr 1022419954 0 0 0 159719900 0 16265892 4846825 5 5 5 6 0 0 497 24817167 17 176595 1352 0 28375276 0 0 0 0 5239 698 0 0 0 0 0 0 3212852 0 12195284 0 0 0 0 0 43 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 12513 2743129 375 12477726 0 0 0 0 37 1351794 0 36 8 0 0 0 0 0 0 5846 0 0 0 0 0 0 0 0 0 141 32 0 55 0 0 0 0 0 0 0 0 18 0 18 0 0 0 0 0 0 66 0 0 0 0 0 0 0 77 0 166 0 0 0 0 0 394 0 0 0 0 0 1339137 0 0 0 0 0 0 313 0 0 0 55759 7 7 7 0 0 0 0 0 0 0 0 3066136 0 47 0 0 0 2 2 0 0 0 6 8 0 0 0 2 0 462 2952327 35420 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 495589 0 0 0 0 3 27 0 0 0 0 0 0 0 0 0 0 0 0 0 0 37662 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 4760 0 0 97 0 0 0 0 0 0 0 0 0 243 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 4649 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 22355451 0 0 0 14 0 24449357 96 49415 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 17067 780222 3211 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 649346 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
                    "ctxt 1572087931\n" +
                    "btime 1649910663\n" +
                    "processes 230673\n" +
                    "procs_running 6\n" +
                    "procs_blocked 0\n" +
                    "softirq 374327567 12481657 139161248 204829 7276312 2275183 26796 12851725 80988196 1422751 117638870";
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, info.getBytes(StandardCharsets.UTF_8)));
        }
        return null;
    }
}