package com.xunmeng.pinduoduo;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * 官方案例
 * <a href="https://github.com/zhkl0228/unidbg/blob/4e7c3c1f478ed9c97c30fff16995105bf949f40d/src/test/java/com/xunmeng/pinduoduo/secure/DeviceNative.java">...</a>
 * 需要插入SIM卡 objection获取设备属性值
 */
public class DeviceNativeTest extends AbstractJni implements IOResolver<AndroidFileIO> {
    private final AndroidEmulator emulator;

    private final VM vm;
    private final Module module;

    private final String className = "com/xunmeng/pinduoduo/secure/DeviceNative";

    private final DvmClass cDeviceNativeTest;

    private static final String ANDROID_ID = "android_id";  // TODO
    private static final int PHONE_TYPE_GSM = 1;

    /**
     * SIM card state: Ready
     */
    private static final int SIM_STATE_READY = 5;

    /**
     * Current network is LTE
     */
    private static final int NETWORK_TYPE_LTE = 13;

    /**
     * Data connection state: Connected. IP traffic should be available.
     */
    private static final int DATA_CONNECTED = 2;

    /**
     * Data connection activity: No traffic.
     */
    private static final int DATA_ACTIVITY_NONE = 0x00000000;

    public DeviceNativeTest() {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .setProcessName("com.xunmeng.pinduoduo")
                .build();
        emulator.getSyscallHandler().addIOResolver(this);
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        memory.setCallInitFunction(true);

        vm = emulator.createDalvikVM((File) null);
        vm.setVerbose(false);
        vm.setJni(this);
        // 加载多个so 依赖
        DalvikModule dm_shared = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/armeabi-v7a/libc++_shared.so"), false);
        DalvikModule dm_UserEnv = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/armeabi-v7a/libUserEnv.so"), false);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/example_binaries/armeabi-v7a/libPddSecure.so"), true);

        dm.callJNI_OnLoad(emulator);
        module = dm.getModule();
        cDeviceNativeTest = vm.resolveClass(className);
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        if ("android/telephony/TelephonyManager->PHONE_TYPE_GSM:I".equals(signature)) {
            return PHONE_TYPE_GSM;
        }

        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        System.out.println("call " + signature);
        switch (signature) {
            case "android/os/Build->SERIAL:Ljava/lang/String;":
                return new StringObject(vm, "05efc1db004d1dee");  // TODO 未提示访问？
            case "android/provider/Settings$Secure->ANDROID_ID:Ljava/lang/String;":
                return new StringObject(vm, ANDROID_ID);
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        System.out.println("call " + signature);
        switch (signature) {
            case "android/provider/Settings$Secure->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;":
                StringObject key = varArg.getObjectArg(1);
                if (ANDROID_ID.equals(key.getValue())) {
                    return new StringObject(vm, "21ad4f5b0bc1b14b");  // TODO
                } else {
                    System.out.println("android/provider/Settings$Secure->getString key=" + key.getValue());
                }
        }

        return callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public void callStaticVoidMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        System.out.println("call " + signature);
        if ("com/tencent/mars/xlog/PLog->i(Ljava/lang/String;Ljava/lang/String;)V".equals(signature)) {
            return;
        }
        super.callStaticVoidMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        System.out.println("call " + signature);
        switch (signature) {
            case "android/os/Debug->isDebuggerConnected()Z":
                return false;
        }
        return super.callStaticBooleanMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        System.out.println("call " + signature);
        switch (signature) {
            case "android/content/Context->checkSelfPermission(Ljava/lang/String;)I":
                return -1;  // 0: 已授权 -1: 未授权
//            case "android/telephony/TelephonyManager->getSimState()I":
//                // https://developer.android.com/reference/android/telephony/TelephonyManager?hl=en#SIM_STATE_UNKNOWN
//                return 5;  // 1: no SIM card  5: Ready
//            case "android/telephony/TelephonyManager->getNetworkType()I":
//                return 13;  // 13: LTE --> 4G
//            case "android/telephony/TelephonyManager->getDataState()I":
//                return 2;  // 指示系统基于运营商信令或运营商特权应用程序启用或禁用运营商数据。运营商数据的打开/关闭不会影响用户设置，但如果设置为false，则会绕过设置并在内部关闭数据。
//            case "android/telephony/TelephonyManager->getDataActivity()I":
//                return 4;  // 数据连接状态：正在断开连接。IP流量可能可用，但将立即停止工作。

            case "android/telephony/TelephonyManager->getPhoneType()I":
                return PHONE_TYPE_GSM;
            case "android/telephony/TelephonyManager->getSimState()I":
                return SIM_STATE_READY;
            case "android/telephony/TelephonyManager->getNetworkType()I":
                return NETWORK_TYPE_LTE;
            case "android/telephony/TelephonyManager->getDataState()I":
                return DATA_CONNECTED;
            case "android/telephony/TelephonyManager->getDataActivity()I":
                return DATA_ACTIVITY_NONE;
        }

        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        System.out.println("call " + signature);
        switch (signature) {
            case "android/content/Context->getSystemService(Ljava/lang/String;)Ljava/lang/Object;":
                DvmClass clazz = vm.resolveClass("android/telephony/TelephonyManager");
                return clazz.newObject(null);

            case "android/telephony/TelephonyManager->getDeviceId()Ljava/lang/String;":
                return new StringObject(vm, "353490069873368");
            case "android/telephony/TelephonyManager->getSimSerialNumber()Ljava/lang/String;":
                return new StringObject(vm, "89860112851083069510");

            case "android/telephony/TelephonyManager->getSimOperatorName()Ljava/lang/String;":
                return new StringObject(vm, "CMCC");  // 中国移动通信
            case "android/telephony/TelephonyManager->getSimCountryIso()Ljava/lang/String;":
                return new StringObject(vm, "cn");

            case "android/telephony/TelephonyManager->getSubscriberId()Ljava/lang/String;":
                return new StringObject(vm, "460013061600183");

            case "android/telephony/TelephonyManager->getNetworkOperator()Ljava/lang/String;":
                // 中国移动：46000 46002 中国联通：46001 中国电信：46003
                return new StringObject(vm, "46000");
            case "android/telephony/TelephonyManager->getNetworkOperatorName()Ljava/lang/String;":
                return new StringObject(vm, "CMCC");
            case "android/telephony/TelephonyManager->getNetworkCountryIso()Ljava/lang/String;":
                return new StringObject(vm, "cn");
            case "android/content/Context->getContentResolver()Landroid/content/ContentResolver;":
                clazz = vm.resolveClass("android/content/ContentResolver");
                return clazz.newObject(null);

            case "java/lang/Throwable->getStackTrace()[Ljava/lang/StackTraceElement;":
                StackTraceElement[] elements = Thread.currentThread().getStackTrace();
                DvmObject<?>[] objs = new DvmObject[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    objs[i] = vm.resolveClass("java/lang/StackTraceElement").newObject(elements[i]);
                }
                return new ArrayObject(objs);
            case "java/lang/StackTraceElement->getClassName()Ljava/lang/String;":
                StackTraceElement element = (StackTraceElement) dvmObject.getValue();
                return new StringObject(vm, element.getClassName());
            case "java/io/ByteArrayOutputStream->toByteArray()[B":
                ByteArrayOutputStream baos = (ByteArrayOutputStream) dvmObject.getValue();
                byte[] data = baos.toByteArray();
                // Inspector.inspect(data, "java/io/ByteArrayOutputStream->toByteArray()");
                return new ByteArray(vm, data);
        }

        return super.callObjectMethod(vm, dvmObject, signature, varArg);

    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "java/lang/Throwable-><init>()V":
                return dvmClass.newObject(null);
            case "java/io/ByteArrayOutputStream-><init>()V":
                return dvmClass.newObject(new ByteArrayOutputStream());
            case "java/util/zip/GZIPOutputStream-><init>(Ljava/io/OutputStream;)V":
                DvmObject<?> obj = varArg.getObjectArg(0);
                OutputStream outputStream = (OutputStream) obj.getValue();
                try {
                    return dvmClass.newObject(new GZIPOutputStream(outputStream));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
        }

        return super.newObject(vm, dvmClass, signature, varArg);
    }

    @Override
    public void callVoidMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/util/zip/GZIPOutputStream->write([B)V":
                OutputStream outputStream = (OutputStream) dvmObject.getValue();
                ByteArray array = varArg.getObjectArg(0);
                // Inspector.inspect(array.getValue(), "java/util/zip/GZIPOutputStream->write outputStream=" + outputStream.getClass().getName());
                try {
                    outputStream.write(array.getValue());
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            case "java/util/zip/GZIPOutputStream->finish()V":
                GZIPOutputStream gzipOutputStream = (GZIPOutputStream) dvmObject.getValue();
                try {
                    gzipOutputStream.finish();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
            case "java/util/zip/GZIPOutputStream->close()V":
                gzipOutputStream = (GZIPOutputStream) dvmObject.getValue();
                try {
                    gzipOutputStream.close();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                return;
        }

        super.callVoidMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        if ("java/util/UUID->randomUUID()Ljava/util/UUID;".equals(signature)) {
            return dvmClass.newObject(UUID.randomUUID());
        }

        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/util/UUID->toString()Ljava/lang/String;":
                UUID uuid = (UUID) dvmObject.getValue();
                return new StringObject(vm, uuid.toString());
            case "java/lang/String->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;":
                StringObject str = (StringObject) dvmObject;
                StringObject s1 = vaList.getObjectArg(0);
                StringObject s2 = vaList.getObjectArg(1);
                return new StringObject(vm, str.getValue().replaceAll(s1.getValue(), s2.getValue()));
        }

        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

//    @Override
//    public FileIO resolve(File workDir, String pathname, int oflags) {
//        if (("/proc/" + emulator.getPid() + "/status").equals(pathname)) {
//            return new ByteArrayFileIO(oflags, pathname, "TracerPid:\t0\n".getBytes());
//        }
//        if ("/proc/version".equals(pathname)) {
//            return new ByteArrayFileIO(oflags, pathname, "Linux version 3.4.0-cyanogenmod+ (zhkl0228@ubuntu) (gcc version 4.7 (GCC) ) #17 SMP PREEMPT Tue Mar 15 18:23:47 CST 2016\n".getBytes());
//        }
//
//        return null;
//    }

//    @Override
//    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
//        if ("/proc/stat".equals(pathname)) {
//            String info = "cpu  15884810 499865 12934024 24971554 59427 3231204 945931 0 0 0\n" +
//                    "cpu0 6702550 170428 5497985 19277857 45380 1821584 529454 0 0 0\n" +
//                    "cpu1 4438333 121907 3285784 1799772 3702 504395 255852 0 0 0\n" +
//                    "cpu2 2735453 133666 2450712 1812564 4626 538114 93763 0 0 0\n" +
//                    "cpu3 2008473 73862 1699542 2081360 5716 367109 66860 0 0 0\n" +
//                    "intr 1022419954 0 0 0 159719900 0 16265892 4846825 5 5 5 6 0 0 497 24817167 17 176595 1352 0 28375276 0 0 0 0 5239 698 0 0 0 0 0 0 3212852 0 12195284 0 0 0 0 0 43 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 12513 2743129 375 12477726 0 0 0 0 37 1351794 0 36 8 0 0 0 0 0 0 5846 0 0 0 0 0 0 0 0 0 141 32 0 55 0 0 0 0 0 0 0 0 18 0 18 0 0 0 0 0 0 66 0 0 0 0 0 0 0 77 0 166 0 0 0 0 0 394 0 0 0 0 0 1339137 0 0 0 0 0 0 313 0 0 0 55759 7 7 7 0 0 0 0 0 0 0 0 3066136 0 47 0 0 0 2 2 0 0 0 6 8 0 0 0 2 0 462 2952327 35420 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 495589 0 0 0 0 3 27 0 0 0 0 0 0 0 0 0 0 0 0 0 0 37662 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 4760 0 0 97 0 0 0 0 0 0 0 0 0 243 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 4649 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 22355451 0 0 0 14 0 24449357 96 49415 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 17067 780222 3211 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 649346 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0\n" +
//                    "ctxt 1572087931\n" +
//                    "btime 1649910663\n" +
//                    "processes 230673\n" +
//                    "procs_running 6\n" +
//                    "procs_blocked 0\n" +
//                    "softirq 374327567 12481657 139161248 204829 7276312 2275183 26796 12851725 80988196 1422751 117638870";
//            return FileResult.success(new ByteArrayFileIO(oflags, pathname, info.getBytes(StandardCharsets.UTF_8)));
//        }
//        return null;
//    }

    @Override
    public FileResult<AndroidFileIO> resolve(Emulator<AndroidFileIO> emulator, String pathname, int oflags) {
        if (("/proc/" + emulator.getPid() + "/status").equals(pathname)) {
//            return new ByteArrayFileIO(oflags, pathname, "TracerPid:\t0\n".getBytes());
            String info = "TracerPid:\t0\n";
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, info.getBytes(StandardCharsets.UTF_8)));
        }
        if ("/proc/version".equals(pathname)) {
//            return new ByteArrayFileIO(oflags, pathname, "Linux version 3.4.0-cyanogenmod+ (zhkl0228@ubuntu) (gcc version 4.7 (GCC) ) #17 SMP PREEMPT Tue Mar 15 18:23:47 CST 2016\n".getBytes());
            String info = "Linux version 3.4.0-cyanogenmod+ (zhkl0228@ubuntu) (gcc version 4.7 (GCC) ) #17 SMP PREEMPT Tue Mar 15 18:23:47 CST 2016\\n";
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, info.getBytes(StandardCharsets.UTF_8)));
        }

        return null;
    }


    public void destroy() throws IOException {
        emulator.close();
    }

    public String info2() {
        String methodSign = "info2(Landroid/content/Context;J)Ljava/lang/String;";
        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null);
        long j = 1688018564388L;
        StringObject obj = cDeviceNativeTest.callStaticJniMethodObject(emulator, methodSign, context, j);
        return obj.getValue();
    }

    public static void main(String[] args) throws Exception {
        DeviceNativeTest deviceNativeTest = new DeviceNativeTest();
        String anti_token = deviceNativeTest.info2();
        System.out.println("anti_token=" + anti_token);
        deviceNativeTest.destroy();
    }
}
