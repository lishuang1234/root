package com.baidu_lishuang10.util;


import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by baidu_lishuang10 on 15/8/4.
 */
public class VirtualTerminal {


    private Process process;
    private DataOutputStream toProcess;

    private ByteArrayOutputStream inpBuffer = new ByteArrayOutputStream();
    private ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

    private InputReader inpReader;
    private InputReader errReader;

    private final Object readLock = new Object();
    private final Object writeLock = new Object();


    public VirtualTerminal() throws IOException, InterruptedException {
        showLog("检测3");
        process = Runtime.getRuntime().exec("su");
        showLog("检测3");

        toProcess = new DataOutputStream(process.getOutputStream());

        inpReader = new InputReader(process.getInputStream(), inpBuffer);
        errReader = new InputReader(process.getErrorStream(), errBuffer);
        //新线程读取输入消息
        Thread.sleep(10);
        inpReader.start();
        errReader.start();
    }

    public void showLog(String infor) {
        Log.e("VT", infor);
    }

    /**
     * 执行shell命令并获取执行结果
     *
     * @param command
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws BrokenPipeException
     */
    public VTCommandResult runCommand(String command) throws IOException, InterruptedException, BrokenPipeException {
        showLog("检测2");
        System.out.println("检测2");
        synchronized (writeLock) {
            inpBuffer.reset();
            errBuffer.reset();
        }
        //注意此处echo代表控制台输出。:RET=$?中的$?代表当前进程pid
        toProcess.writeBytes(command + "\necho :RET=$?\n");
        toProcess.flush();
        while (true) {//循环读取输入信息
            synchronized (readLock) {
                boolean doWait;
                synchronized (writeLock) {
                    byte[] inpbyte = inpBuffer.toByteArray();
                    String inp = new String(inpbyte);
                    doWait = !inp.contains(":RET=");
                }
                showLog("检测1");
                if (doWait)
                    readLock.wait();//此时无反馈信息，阻塞
            }
            synchronized (writeLock) {
                byte[] inpbyte = inpBuffer.toByteArray();
                byte[] errbyte = errBuffer.toByteArray();

                String inp = new String(inpbyte);
                String err = new String(errbyte);
                showLog("VT得到的信息：" + inp);
                if (inp.contains(":RET=")) {
                    if (inp.contains(":RET=EOF") || err.contains(":RET=EOF"))
                        throw new BrokenPipeException();
                    if (inp.contains(":RET=0"))//此时pid为0，root进程
                        return new VTCommandResult(0, inp, err);
                    else
                        return new VTCommandResult(1, inp, err);
                }
            }
        }
    }


    /**
     * 获取命令反馈信息
     */
    private class InputReader extends Thread {
        private InputStream inpStream;
        private ByteArrayOutputStream baoStream;

        public InputReader(InputStream inputStream, ByteArrayOutputStream byteArrayOutputStream) {
            this.inpStream = inputStream;
            this.baoStream = byteArrayOutputStream;

        }

        @Override
        public void run() {
            byte[] temp = new byte[1024];
            try {
                while (true) {//循环拿到反馈信息
                    int length = inpStream.read(temp);
                    if (length < 0) {//信息输入错误
                        synchronized (writeLock) {
                            temp = ":RET=EOF".getBytes();
                            baoStream.write(temp);
                        }
                        synchronized (readLock) {
                            readLock.notifyAll();
                        }
                        return;
                    }
                    if (length > 0) {//有信息输入
                        synchronized (writeLock) {
                            baoStream.write(temp, 0, length);
                        }
                        synchronized (readLock) {
                            readLock.notifyAll();
                        }
                    }
                    //注意无信息输入时，不做处理
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        inpReader.interrupt();
        errReader.interrupt();
        process.destroy();
    }


    //封装命令执行结果
    public class VTCommandResult {
        private String stdout;
        private String stderr;
        private Integer exitValue;

        public VTCommandResult(Integer exitValue) {
            this(exitValue, null, null);
        }

        public VTCommandResult(Integer exitValue, String stdout, String stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitValue = exitValue;
        }

        public boolean success() {
            return exitValue != null && exitValue == 0;
        }

    }

    public class BrokenPipeException extends Exception {
        private static final long serialVersionUID = 1L;

        public BrokenPipeException() {
        }
    }
}
