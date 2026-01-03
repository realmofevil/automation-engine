package dev.realmofevil.automation.engine.db;

import java.sql.Connection;

public final class TransactionManager {

    private final OperatorDbPool pool;
    private final ThreadLocal<TransactionContext> current =
            new ThreadLocal<>();

    public TransactionManager(OperatorDbPool pool) {
        this.pool = pool;
    }

    public TransactionContext begin() {
        //TestReporter.info("Opening DB transaction");
        Connection conn = pool.acquire();
        TransactionContext ctx = new TransactionContext(conn);
        current.set(ctx);
        return ctx;
    }

    public TransactionContext current() {
        return current.get();
    }

    public void end() {
        TransactionContext ctx = current.get();
        if (ctx == null) {
            return;
        }

        try {
            if (ctx.shouldCommit()) {
                //TestReporter.info("Committing DB transaction");
                ctx.connection().commit();
            } else {
                ctx.connection().rollback();
            }
        } catch (Exception e) {
            //TestReporter.warn("Rolling back DB transaction");
            throw new RuntimeException("Transaction failed", e);
        } finally {
            pool.release(ctx.connection());
            current.remove();
        }
    }
}