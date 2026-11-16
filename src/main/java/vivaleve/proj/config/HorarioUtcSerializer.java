package vivaleve.proj.config;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Todo LocalDateTime (createdAt, updatedAt, lastMessageAt...) sai no JSON em UTC e com "Z":
 * "2026-11-16T19:20:19.270Z".
 *
 * Por quê: as datas são gravadas sem fuso no horário do servidor. O Render roda em UTC, e sem o "Z"
 * o navegador lia "19:20" como horário de Brasília, mostrando tudo 3 horas adiantado.
 * Com o "Z", o new Date(...) do front converte para o fuso de quem está usando o app.
 *
 * Os dados antigos do banco não mudam: já estão em UTC (ver DemoApplication, que fixa o fuso do
 * servidor em UTC também quando o back roda num computador no Brasil).
 */
@JacksonComponent
public class HorarioUtcSerializer extends StdSerializer<LocalDateTime> {

    private static final DateTimeFormatter ISO_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public HorarioUtcSerializer() {
        super(LocalDateTime.class);
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
        gen.writeString(paraUtc(value));
    }

    /** Interpreta o valor no fuso do servidor e escreve em UTC. */
    static String paraUtc(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(ISO_UTC);
    }
}
