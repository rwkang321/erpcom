package com.erpcom.erpcom;
import org.junit.jupiter.api.Test;
import com.erpcom.erpcom.common.message.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class MessageOutageTests {
    @Test void databaseOutageHasSafeFallbackWithoutRecursion() {
        MessageMapper mapper = mock(MessageMapper.class);
        when(mapper.languages()).thenThrow(new IllegalStateException("DB unavailable"));
        var service = new MessageServiceImpl(mapper);
        assertThat(service.get("ERR_ACCESS_DENIED","ko").httpStatus()).isEqualTo(403);
        assertThat(service.language(null,null,"en")).isEqualTo("ko");
        verify(mapper,times(2)).languages();
        verifyNoMoreInteractions(mapper);
    }
}
