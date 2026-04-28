package com.bae4.drivedrop.service;

import com.bae4.drivedrop.entity.AccessCode;
import com.bae4.drivedrop.entity.ShareTask;
import com.bae4.drivedrop.enums.ShareMode;
import com.bae4.drivedrop.repository.AccessCodeRepository;
import com.bae4.drivedrop.repository.ShareTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class AccessCodeConcurrencyTest {

    @Autowired
    private AccessCodeService accessCodeService;

    @Autowired
    private ShareTaskRepository taskRepository;

    @Test
    void testConcurrentClaim() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    accessCodeService.consumeAccessCode(testTaskId, null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                    // System.err.println("抢票失败: " + e.getMessage());
                    failCount.incrementAndGet();
                }
            });
        }

        latch.countDown();

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);

        if (finished) {
            System.out.println("成功人数: " + successCount.get());
            System.out.println("失败人数: " + failCount.get());

            assertEquals(3, successCount.get());
            assertEquals(7, failCount.get());
        } else {
            fail("测试超时，可能发生了死锁");
        }
    }

    @Autowired
    private AccessCodeRepository codeRepository;

    private String testTaskId;

    @BeforeEach
    void setup() {
        codeRepository.deleteAll();
        taskRepository.deleteAll();

        ShareTask task = new ShareTask();
        task.setGoogleFileId("mock-google-file-id-123");
        task.setShareMode(ShareMode.OPEN_CLAIM);

        // 先保存 Task 拿到 ID
        ShareTask savedTask = taskRepository.saveAndFlush(task);
        this.testTaskId = savedTask.getId();

        AccessCode code = new AccessCode();
        code.setCode("LUCKY-CODE");
        code.setAssigned(false);
        code.setTask(savedTask);

        AccessCode code02 = new AccessCode();
        code02.setCode("LUCKY-CODE02");
        code02.setAssigned(false);
        code02.setTask(savedTask);

        AccessCode code03 = new AccessCode();
        code03.setCode("LUCKY-CODE03");
        code03.setAssigned(false);
        code03.setTask(savedTask);

        codeRepository.saveAndFlush(code);
        codeRepository.saveAndFlush(code02);
        codeRepository.saveAndFlush(code03);

        System.out.println("数据准备完毕，Task ID: " + testTaskId + "，已存入3个名额");
    }
}
