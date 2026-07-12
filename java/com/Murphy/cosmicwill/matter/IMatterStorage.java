package com.Murphy.cosmicwill.matter;

public interface IMatterStorage {

    long getMatter();

    long getCapacity();

    /**
     * @return 实际接收的 MU。
     */
    long insertMatter(long amount, boolean simulate);

    /**
     * @return 实际提取的 MU。
     */
    long extractMatter(long amount, boolean simulate);
}
