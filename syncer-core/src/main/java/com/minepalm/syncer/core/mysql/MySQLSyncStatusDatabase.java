package com.minepalm.syncer.core.mysql;

import kr.msleague.mslibrary.database.impl.internal.MySQLDatabase;
import lombok.RequiredArgsConstructor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class MySQLSyncStatusDatabase {

    // key (String) / holder proxy / holder server / stage / timeout

    private final String table;
    private final MySQLDatabase database;

    public void init(){
        this.database.execute(connection -> {
            PreparedStatement ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + table + " ( " +
                    "`row_id` BIGINT AUTO_INCREMENT UNIQUE, " +
                    "`objectId` VARCHAR(64), " +
                    "`server` VARCHAR(32), " +
                    "`timeout` BIGINT DEFAULT 0, " +
                    "PRIMARY KEY(`objectId`)) " +
                    "charset=utf8mb4");
            ps.execute();

        });
    }

    public void initProcedures(){
        this.database.execute(connection -> {
            PreparedStatement dropCreateProcedureIfExists = connection.prepareStatement("DROP PROCEDURE IF EXISTS updateHold");
            dropCreateProcedureIfExists.execute();
            PreparedStatement createProcedure = connection.prepareStatement("" +
                    "CREATE PROCEDURE updateHold " +
                    "(" +
                    "m_objectId VARCHAR(64), " +
                    "m_serverIn VARCHAR(32), " +
                    "m_currentTime BIGINT, " +
                    "m_timeoutDuration BIGINT " +
                    ")" +
                    "BEGIN " +
                    "DECLARE m_serverName VARCHAR(32); " +
                    "DECLARE m_timeout BIGINT; "+
                    "DECLARE m_isHeld BOOLEAN; "+
                    "DECLARE m_isTimeout BOOLEAN; " +
                    "DECLARE result BOOLEAN; "+
                    " "+
                    "START TRANSACTION; "+
                    " "+
                    "SELECT timeout INTO m_timeout FROM syncer_status WHERE objectId=m_objectId FOR UPDATE; "+
                    "SELECT server INTO m_serverName FROM syncer_status WHERE objectId=m_objectId FOR UPDATE; "+
                    " "+
                    "SET m_serverName = IF(ISNULL(m_serverName), m_serverIn, m_serverName); "+
                    "SET m_timeout = IF(ISNULL(m_timeout), 0, m_timeout); "+
                    " "+
                    "SET m_isHeld = IF(m_serverIn = m_serverName, true, false); " +
                    "SET m_isTimeout = IF(m_currentTime >= m_timeout, true, false); " +
                    "IF m_isHeld OR m_isTimeout " +
                    "THEN "+
                    "   INSERT INTO syncer_status " +
                    "       (objectId, server, timeout) " +
                    "       VALUES(m_objectId, m_serverIn, (m_currentTime + m_timeoutDuration)) " +
                    "       ON DUPLICATE KEY UPDATE " +
                    "       server=VALUES(server), " +
                    "       timeout=VALUES(timeout);" +
                    "   SET result = TRUE; "+
                    "ELSE "+
                    "   SET result = FALSE; "+
                    "END IF; "+
                    " "+
                    "COMMIT; "+
                    "SELECT result; "+
                    " "+
                    "END;");
            createProcedure.execute();

            PreparedStatement dropReleaseProcedure = connection.prepareStatement("DROP PROCEDURE IF EXISTS releaseHold;");
            dropReleaseProcedure.execute();

            PreparedStatement releaseProcedure = connection.prepareStatement("" +
                    "CREATE PROCEDURE releaseHold " +
                    "(" +
                    "m_objectId VARCHAR(64), " +
                    "m_serverIn VARCHAR(32), " +
                    "m_currentTime BIGINT " +
                    ")" +
                    "BEGIN " +
                    "DECLARE m_serverName VARCHAR(32); " +
                    "DECLARE m_timeout BIGINT; "+
                    "DECLARE m_isHeld BOOLEAN; "+
                    "DECLARE m_isTimeout BOOLEAN; " +
                    "DECLARE result BOOLEAN; "+
                    " "+
                    "START TRANSACTION; "+
                    " "+
                    "SELECT timeout INTO m_timeout FROM syncer_status WHERE objectId=m_objectId FOR UPDATE; "+
                    "SELECT server INTO m_serverName FROM syncer_status WHERE objectId=m_objectId FOR UPDATE; "+
                    " "+
                    "SET m_serverName = IF(ISNULL(m_serverName), m_serverIn, m_serverName); "+
                    "SET m_timeout = IF(ISNULL(m_timeout), 0, m_timeout); "+
                    " "+
                    "SET m_isHeld = IF(m_serverIn = m_serverName, true, false); " +
                    "SET m_isTimeout = IF(m_currentTime >= m_timeout, true, false); " +

                    "IF m_isHeld OR m_isTimeout " +
                    "THEN "+
                    "   DELETE FROM syncer_status WHERE objectId=m_objectId;" +
                    "   SET result = TRUE; "+
                    "ELSE "+
                    "   SET result = FALSE; "+
                    "END IF; "+
                    " "+
                    "COMMIT; "+
                    "SELECT result; "+
                    " "+
                    "END; ");
            releaseProcedure.execute();

            PreparedStatement dropUpdateProcedureIfExists = connection.prepareStatement("DROP PROCEDURE IF EXISTS addTimeout;");
            dropUpdateProcedureIfExists.execute();

            PreparedStatement updateTimeoutProcedure = connection.prepareStatement("" +
                    "CREATE PROCEDURE addTimeout " +
                    "(" +
                    "m_objectId VARCHAR(64), " +
                    "m_serverIn VARCHAR(32), " +
                    "m_currentTime BIGINT, " +
                    "m_addTimeout BIGINT " +
                    ")" +
                    "BEGIN " +
                    "DECLARE m_serverName VARCHAR(32); " +
                    "DECLARE m_timeout BIGINT; "+
                    "DECLARE m_isHeld BOOLEAN; "+
                    "DECLARE m_isTimeout BOOLEAN; " +
                    "DECLARE result BOOLEAN; "+
                    " "+
                    "START TRANSACTION; "+
                    " "+
                    "SELECT timeout INTO m_timeout FROM syncer_status WHERE objectId=m_objectId; "+
                    "SELECT server INTO m_serverName FROM syncer_status WHERE objectId=m_objectId; "+
                    " "+
                    "SET m_serverName = IF(ISNULL(m_serverName), m_serverIn, m_serverName); "+
                    "SET m_timeout = IF(ISNULL(m_timeout), 0, m_timeout); "+
                    " "+
                    "SET m_isHeld = IF(m_serverIn = m_serverName, true, false); " +
                    "SET m_isTimeout = IF(m_currentTime >= m_timeout, true, false); " +

                    "IF m_isHeld OR m_isTimeout " +
                    "THEN "+
                    "   UPDATE syncer_status SET timeout = timeout + m_addTimeout WHERE objectId=m_objectId;" +
                    "   SET result = TRUE; "+
                    "ELSE "+
                    "   SET result = FALSE; "+
                    "END IF; "+
                    " "+
                    "COMMIT; "+
                    "SELECT result; "+
                    " "+
                    "END;");
            updateTimeoutProcedure.execute();
        });
    }

    public CompletableFuture<Void> releaseAll(String server){
        return database.executeAsync(connection -> {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM syncer_status WHERE `server`=?");
            ps.setString(1, server);
            ps.execute();
            return null;
        });
    }

    CompletableFuture<Boolean> isHeldServer(String server, String objectId){
        return database.executeAsync(connection -> {
            PreparedStatement ps = connection.prepareStatement("SELECT `server` FROM syncer_status WHERE `objectId`=?");
            ps.setString(1, objectId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getString(1).equals(server);
            }else{
                return false;
            }
        });
    }

    CompletableFuture<String> getHoldingServer(String objectId){
        return database.executeAsync(connection -> {
            PreparedStatement ps = connection.prepareStatement("SELECT `server` FROM syncer_status WHERE `objectId`=?");
            ps.setString(1, objectId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getString(1);
            }else{
                return null;
            }
        });
    }

    CompletableFuture<Boolean> hold(String objectId, String server, long currentTime, long holdingDuration){
        return database.executeAsync(connection -> {
            try {
                connection.setAutoCommit(false);
                PreparedStatement ps2 = connection.prepareStatement("CALL `updateHold`(?, ?, ?, ?)");
                ps2.setString(1, objectId);
                ps2.setString(2, server);
                ps2.setLong(3, currentTime);
                ps2.setLong(4, holdingDuration);
                ResultSet rs = ps2.executeQuery();
                connection.commit();
                if(rs.next()){
                    return rs.getBoolean(1);
                }else{
                    return true;
                }
            }catch (SQLException e){
                connection.rollback();
                return false;
            }finally {
                connection.setAutoCommit(true);
            }
        });
    }

    CompletableFuture<Void> holdUnsafe(String objectId, String server, long time){
        return database.executeAsync(connection -> {
            try {
                connection.setAutoCommit(false);
                PreparedStatement ps2 = connection.prepareStatement("INSERT INTO syncer_status "+
                        "(`objectId`, `server`, `timeout`) " +
                        "VALUES(?, ?, ?)" +
                        "ON DUPLICATE KEY UPDATE " +
                        "`server`=VALUES(`server`), " +
                        "`timeout`=VALUES(`timeout`);");
                ps2.setString(1, objectId);
                ps2.setString(2, server);
                ps2.setLong(3, time);
                ps2.execute();
                connection.commit();
            }finally {
                connection.setAutoCommit(true);
            }
            return null;
        });
    }

    CompletableFuture<Boolean> release(String objectId, String server, long time){
        return database.executeAsync(connection -> {
            try {
                connection.setAutoCommit(false);
                PreparedStatement ps2 = connection.prepareStatement("CALL `releaseHold`(?, ?, ?)");
                ps2.setString(1, objectId);
                ps2.setString(2, server);
                ps2.setLong(3, time);
                ResultSet rs = ps2.executeQuery();
                connection.commit();
                if(rs.next()){
                    return rs.getBoolean(1);
                }else{
                    return true;
                }
            }finally {
                connection.setAutoCommit(true);
            }
        });
    }

    CompletableFuture<Void> releaseUnsafe(String objectId){
        return database.executeAsync(connection -> {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM syncer_status WHERE `objectId`=?");
            ps.setString(1, objectId);
            ps.execute();
            return null;
        });
    }

    CompletableFuture<Boolean> updateTimeout(String objectId, String server, long time){
        return database.executeAsync(connection -> {
            try {
                connection.setAutoCommit(false);
                PreparedStatement ps2 = connection.prepareStatement("CALL `addTimeout`(?, ?, ?, ?)");
                ps2.setString(1, objectId);
                ps2.setString(2, server);
                ps2.setLong(3, System.currentTimeMillis());
                ps2.setLong(4, time);
                ResultSet rs = ps2.executeQuery();
                connection.commit();
                if(rs.next()){
                    return rs.getBoolean(1);
                }else{
                    return true;
                }
            }finally {
                connection.setAutoCommit(true);
            }
        });
    }

    CompletableFuture<Boolean> setTimeout(String objectId, String server, long time) {
        return database.executeAsync(connection -> {
            try {
                connection.setAutoCommit(false);
                PreparedStatement ps2 = connection.prepareStatement("INSERT INTO syncer_status "+
                        "(`objectId`, `server`, `timeout`) " +
                        "VALUES(?, ?, ?)" +
                        "ON DUPLICATE KEY UPDATE " +
                        "`server`=VALUES(`server`), " +
                        "`timeout`=VALUES(`timeout`);");
                ps2.setString(1, objectId);
                ps2.setString(2, server);
                ps2.setLong(3, time);
                connection.commit();
            }finally {
                connection.setAutoCommit(true);
            }
            return null;
        }).thenCompose(ignore -> equals(objectId, server, time));
    }

    CompletableFuture<Boolean> equals(String objectId, String server, long time){
        return database.executeAsync(connection -> {
            PreparedStatement ps = connection.prepareStatement("SELECT `timeout` FROM syncer_status WHERE `objectId`=? AND `server`=?");
            ps.setString(1, objectId);
            ps.setString(2, server);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getLong(1) == time;
            }else{
                return false;
            }
        });
    }
}
