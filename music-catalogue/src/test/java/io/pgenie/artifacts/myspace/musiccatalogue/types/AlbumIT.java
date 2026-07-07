package io.pgenie.artifacts.myspace.musiccatalogue.types;

import static org.junit.jupiter.api.Assertions.*;

import io.pgenie.artifacts.myspace.musiccatalogue.AbstractDatabaseIT;
import io.codemine.java.postgresql.jdbc.Statement;
import java.sql.*;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AlbumIT extends AbstractDatabaseIT {

    private Optional<Album> roundtrip(Optional<Album> input) throws SQLException {
        return execute(new Statement<Optional<Album>>() {
            @Override public String sql() { return "select ?::album"; }
            @Override public void bindParams(PreparedStatement ps) throws SQLException {
                Album.CODEC.bind(ps, 1, input.orElse(null));
            }
            @Override public boolean returnsRows() { return true; }
            @Override public Optional<Album> decodeResultSet(ResultSet rs) throws SQLException {
                rs.next();
                return Album.CODEC.decodeOptional(rs, 0, 1);
            }
            @Override public Optional<Album> decodeAffectedRows(long r) {
                throw new UnsupportedOperationException();
            }
        });
    }
    

    @Test

    void roundtripNull() throws Exception {
        assertEquals(Optional.empty(), roundtrip(Optional.empty()));
    }
    

    @Test

    void roundtripCombination0() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination1() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination2() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination3() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination4() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination5() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination6() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination7() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination8() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination9() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination10() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination11() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination12() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination13() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination14() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination15() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination16() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination17() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination18() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination19() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination20() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination21() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination22() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination23() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination24() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination25() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination26() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination27() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination28() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination29() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination30() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination31() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination32() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination33() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination34() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination35() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination36() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination37() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination38() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination39() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination40() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination41() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination42() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination43() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination44() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination45() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination46() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination47() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination48() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination49() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination50() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination51() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination52() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination53() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination54() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination55() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination56() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination57() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination58() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination59() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination60() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination61() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination62() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination63() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination64() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination65() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination66() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination67() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination68() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination69() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination70() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination71() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination72() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination73() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination74() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination75() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination76() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination77() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination78() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination79() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination80() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination81() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination82() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination83() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination84() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination85() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination86() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination87() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination88() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination89() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination90() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination91() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination92() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination93() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination94() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination95() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination96() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination97() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination98() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination99() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination100() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination101() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination102() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination103() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination104() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination105() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination106() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination107() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination108() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination109() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination110() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination111() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination112() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination113() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination114() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination115() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination116() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination117() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination118() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination119() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination120() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination121() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination122() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination123() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination124() throws Exception {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination125() throws Exception {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination126() throws Exception {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test

    void roundtripCombination127() throws Exception {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }
}
