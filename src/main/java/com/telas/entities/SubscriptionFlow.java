package com.telas.entities;

import com.telas.enums.CodeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "verification_codes")
@NoArgsConstructor
public class VerificationCode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1084934057135367842L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(name = "code")
    private String code;

    @Column(name = "type", columnDefinition = "code_type")
    @Enumerated(EnumType.STRING)
    private CodeType codeType;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant expiresAt;

    @Column(name = "fl_validated")
    private boolean validated = false;

    public VerificationCode(String code, Instant expiresAt, CodeType type) {
        this.code = code;
        this.expiresAt = expiresAt;
        codeType = type;
    }

}
