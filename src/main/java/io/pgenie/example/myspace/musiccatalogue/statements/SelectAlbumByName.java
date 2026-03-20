package io.pgenie.example.myspace.musiccatalogue.statements;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import io.pgenie.example.myspace.musiccatalogue.Statement;

/**
 * Statement that selects an album id by name, or returns null when absent.
 */
public final class SelectAlbumByName implements Statement<Long> {

    private final String name;

    public SelectAlbumByName(String name) {
        this.name = name;
    }

    @Override
    public String sql() {
        return "select id from album where name = ? limit 1";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
        ps.setString(1, name);
    }

    @Override
    public boolean returnsRows() {
        return true;
    }

    @Override
    public Long decodeResultSet(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return rs.getLong(1);
        }
        return null;
    }

    @Override
    public Long decodeAffectedRows(long affectedRows) {
        throw new UnsupportedOperationException();
    }
}
