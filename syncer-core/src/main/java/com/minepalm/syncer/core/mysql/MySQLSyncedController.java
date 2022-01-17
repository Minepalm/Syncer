package com.minepalm.syncer.core.mysql;

import com.minepalm.syncer.api.SyncStage;
import com.minepalm.syncer.core.HoldData;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
public class MySQLSyncedController {

    private final String objectId;
    protected final MySQLSyncStatusDatabase database;

    public CompletableFuture<SyncStage> getStage() {
        return database.getStage(objectId);
    }

    public CompletableFuture<String> getHoldProxy() {
        return database.getHoldingProxy(objectId);
    }

    public CompletableFuture<String> getHoldServer() {
        return database.getHoldingServer(objectId);
    }

    public CompletableFuture<Boolean> isHold(String holderName) {
        return database.isHeldServer(holderName, objectId);
    }

    public CompletableFuture<Boolean> tryHoldAsync(HoldData data, long timeout){
        return database.hold(objectId, data, timeout);
    }

    public boolean tryHold(HoldData data, long timeout) throws ExecutionException, InterruptedException {
        return database.hold(objectId, data, timeout).get();
    }

    public void holdUnsafe(HoldData data) throws ExecutionException, InterruptedException {
        database.holdUnsafe(objectId, data).get();
    }

    public CompletableFuture<Void> holdUnsafeAsync(HoldData data){
        return database.holdUnsafe(objectId, data);
    }

    public CompletableFuture<Boolean> releaseAsync(HoldData data){
        return database.release(objectId, data);
    }

    public boolean release(HoldData data) throws ExecutionException, InterruptedException {
        return database.release(objectId, data).get();
    }

    public CompletableFuture<Void> releaseUnsafeAsync(){
        return database.releaseUnsafe(objectId);
    }

    public void releaseUnsafe() throws ExecutionException, InterruptedException {
        database.releaseUnsafe(objectId).get();
    }
}