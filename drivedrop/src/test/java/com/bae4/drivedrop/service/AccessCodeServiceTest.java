package com.bae4.drivedrop.service;

import com.bae4.drivedrop.entity.AccessCode;
import com.bae4.drivedrop.entity.ShareTask;
import com.bae4.drivedrop.enums.ShareMode;
import com.bae4.drivedrop.repository.AccessCodeRepository;
import com.bae4.drivedrop.repository.ShareTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
class AccessCodeServiceTest {

    @Autowired
    private ShareTaskRepository taskRepository;
    @Autowired
    private AccessCodeRepository codeRepository;

    @Autowired
    private AccessCodeService accessCodeService;

    private String testTaskId;
    private final String secretKey = "key-ABC";
    private final String fileId = "google-file-id";

    @Test
    void shouldConsumeSpecificKey() {
        ShareTask task = accessCodeService.consumeAccessCode(testTaskId, secretKey).getTask();

        assertEquals(fileId, task.getGoogleFileId());
        assertThrows(RuntimeException.class, () -> accessCodeService.consumeAccessCode(testTaskId, secretKey));
    }

    @Test
    void testConcurrentClaim() throws InterruptedException {
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    accessCodeService.consumeAccessCode(testTaskId, secretKey);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
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

            assertEquals(1, successCount.get(), "必须只有1个人抢到");
            assertEquals(2, failCount.get(), "必须有9个人由于锁竞争或无票而失败");
        } else {
            fail("测试超时，可能发生了死锁");
        }
    }

    @BeforeEach
    void setup() {
        codeRepository.deleteAll();
        taskRepository.deleteAll();

        ShareTask task = new ShareTask();
        task.setGoogleFileId(fileId);
        task.setShareMode(ShareMode.DISTRIBUTED);

        ShareTask savedTask = taskRepository.saveAndFlush(task);
        this.testTaskId = savedTask.getId();

        AccessCode code = new AccessCode();
        code.setCode(secretKey);
        code.setAssigned(false);
        code.setTask(savedTask);
        codeRepository.saveAndFlush(code);


        System.out.println("数据准备完毕，Task ID: " + testTaskId + "，已存入1个名额");
    }
}


