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

    private Optional<Album> roundtrip(Optional<Album> input) {
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
    void roundtripNull() {
        assertEquals(Optional.empty(), roundtrip(Optional.empty()));
    }
    

    @Test
    void roundtripCombination0() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination1() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination2() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination3() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination4() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination5() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination6() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination7() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination8() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination9() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination10() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination11() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination12() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination13() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination14() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination15() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination16() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination17() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination18() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination19() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination20() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination21() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination22() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination23() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination24() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination25() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination26() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination27() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination28() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination29() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination30() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination31() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination32() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination33() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination34() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination35() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination36() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination37() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination38() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination39() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination40() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination41() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination42() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination43() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination44() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination45() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination46() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination47() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination48() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination49() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination50() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination51() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination52() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination53() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination54() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination55() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination56() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination57() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination58() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination59() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination60() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination61() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination62() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination63() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.empty());
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination64() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination65() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination66() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination67() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination68() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination69() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination70() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination71() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination72() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination73() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination74() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination75() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination76() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination77() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination78() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination79() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination80() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination81() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination82() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination83() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination84() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination85() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination86() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination87() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination88() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination89() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination90() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination91() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination92() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination93() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination94() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination95() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination96() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination97() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination98() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination99() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination100() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination101() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination102() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination103() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination104() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination105() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination106() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination107() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination108() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination109() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination110() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination111() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.empty(), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination112() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination113() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination114() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination115() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination116() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination117() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination118() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination119() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.empty(), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination120() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination121() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination122() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination123() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.empty(), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination124() {
        var value = new Album(Optional.empty(), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination125() {
        var value = new Album(Optional.of(0L), Optional.empty(), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination126() {
        var value = new Album(Optional.empty(), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }

    @Test
    void roundtripCombination127() {
        var value = new Album(Optional.of(0L), Optional.of(""), Optional.of(LocalDate.of(2000, 1, 1)), Optional.of(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)), Optional.of(List.of()), Optional.of(DiscInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertEquals(Optional.of(value), roundtrip(Optional.of(value)));
    }
}
