package org.hein.sequence.snowflake;

/**
 * 雪花算法标识位包装器
 */
public class IdentifyWrapper {

    /**
     * 工作 ID
     */
    private Long workerId;

    /**
     * 数据中心 ID
     */
    private Long dataCenterId;

    public IdentifyWrapper() {
    }

    public IdentifyWrapper(Long workerId, Long dataCenterId) {
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public Long getDataCenterId() {
        return dataCenterId;
    }

    public void setDataCenterId(Long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }
}
