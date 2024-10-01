package org.hein.sequence.snowflake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 雪花算法
 */
public class Snowflake {

    private static final Logger logger = LoggerFactory.getLogger(Snowflake.class);

    /**
     * 时间起始标记点，作为基准，一般取系统的最近时间（一旦确定不能变动）
     */
    private final long twepoch = 1288834974657L;

    /**
     * 机器标识位数
     */
    private final long workerIdBits = 3L;
    private final long datacenterIdBits = 4L;
    private final long maxWorkerId = ~(-1L << workerIdBits);
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);

    /**
     * 时钟序列位
     */
    private final long clockSequenceBits = 3L;

    /**
     * 毫秒内自增位
     */
    private final long sequenceBits = 12L;

    private final long sequenceShift = 0L;
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long clockSequenceShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits + clockSequenceBits;

    private final long sequenceMask = ~(-1L << sequenceBits);
    private final long clockSequenceMask = ~(-1L << clockSequenceBits);

    /**
     * workerId 部分
     */
    private final long workerId;

    /**
     * 数据标识 ID 部分
     */
    private final long datacenterId;

    /**
     * 并发控制
     */
    private long sequence = 0L;

    /**
     * 时钟序列
     */
    private long clockSequence = 0L;

    /**
     * 上次生成 ID 时间戳
     */
    private long lastTimestamp = -1L;

    private InetAddress inetAddress;

    public Snowflake() {
        this(null);
    }

    public Snowflake(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
        this.datacenterId = getDataCenterId(maxDatacenterId);
        this.workerId = getWorkerId(datacenterId, maxWorkerId);
    }

    public Snowflake(long workerId, long dataCenterId) {
        this.workerId = workerId;
        this.datacenterId = dataCenterId;
    }

    /**
     * 获取 workerId
     */
    private long getWorkerId(long datacenterId, long maxWorkerId) {
        StringBuilder mpid = new StringBuilder();
        mpid.append(datacenterId);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (name != null && !name.isEmpty()) {
            /*
             * GET jvmPid
             */
            mpid.append(name.split("@")[0]);
        }
        /*
         * MAC + PID 的 hashcode 获取 16 个低位
         */
        return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    /**
     * 数据标识 id 部分
     */
    private long getDataCenterId(long maxDataCenterId) {
        long id = 0L;
        try {
            if (null == this.inetAddress) {
                this.inetAddress = InetAddress.getLocalHost();
            }
            NetworkInterface network = NetworkInterface.getByInetAddress(this.inetAddress);
            if (null == network) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    id = ((0x000000FF & (long) mac[mac.length - 2]) | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    id = id % (maxDataCenterId + 1);
                }
            }
        } catch (Exception e) {
            logger.warn(" getDataCenterId: {}", e.getMessage());
        }
        return id;
    }

    /**
     * 获取下一个 id
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        return forceFixOffset(timestamp);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                return forceFixOffset(timestamp);
            }
        }

        if (lastTimestamp == timestamp) {
            // 相同毫秒内，序列号自增
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 同一毫秒的序列数已经达到最大
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒内，序列号置为 1 - 3 随机数
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }

        lastTimestamp = timestamp;

        // 时间戳部分 | 时钟序列 | 数据中心部分 | 机器标识部分 | 序列号部分
        return ((timestamp - twepoch) << timestampLeftShift)
                | (clockSequence << clockSequenceShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | (sequence << sequenceShift);
    }

    private long forceFixOffset(long timestamp) {
        // 自增 clockSequence
        clockSequence = (clockSequence + 1) & clockSequenceMask;
        // 强行修正时间戳
        lastTimestamp = timestamp;
        // 时间戳部分 | 时钟序列 | 数据中心部分 | 机器标识部分 | 序列号部分
        return ((timestamp - twepoch) << timestampLeftShift)
                | (clockSequence << clockSequenceShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | (sequence << sequenceShift);
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }
}