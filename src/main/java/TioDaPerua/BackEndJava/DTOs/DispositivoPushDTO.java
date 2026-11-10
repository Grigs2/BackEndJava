package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Token do Expo do aparelho ("ExponentPushToken[...]") e plataforma (ios, android). */
public record DispositivoPushDTO(@NotBlank @Size(max = 200) String expoToken,
                                 @Size(max = 20) String plataforma) {
}
