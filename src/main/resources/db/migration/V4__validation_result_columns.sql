-- =============================================================================
-- ERP FLEET SERVICE - V4: RESULTADO DE LA VALIDACION Y ESTADO DEL REINTENTO
-- =============================================================================
-- La validacion contra el hub devuelve datos que no tenian donde guardarse:
-- la razon social, el estado y la condicion del RUC, la direccion fiscal, el
-- numero de motor, el color y el combustible. Ademas, el reintento
-- programado no tenia con que contar ni cuando volver a intentarlo.
--
-- El estado de validacion es un conjunto explicito: pendiente, validado y
-- observado. "Observado" es la discrepancia que informa el hub; no es un
-- fallo de la validacion, que es otra cosa: un intento que se reintenta.
--
-- Ver docs/doc-estado-implementacion.md, hallazgo P1 6.6.
-- =============================================================================

ALTER TABLE customers ADD COLUMN IF NOT EXISTS razon_social VARCHAR(255);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS estado_ruc VARCHAR(50);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS condicion_ruc VARCHAR(50);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS direccion_fiscal VARCHAR(255);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS validated_at TIMESTAMP;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS validation_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS next_validation_at TIMESTAMP;

ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS numero_motor VARCHAR(100);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS color VARCHAR(50);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS combustible VARCHAR(50);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS validated_at TIMESTAMP;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS validation_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS next_validation_at TIMESTAMP;

-- El barrido de revalidacion busca los registros pendientes cuya proxima
-- validacion ya vencio. El indice parcial cubre exactamente ese conjunto.
CREATE INDEX IF NOT EXISTS idx_vehicle_pending_validation
    ON vehicles(next_validation_at)
    WHERE validation_status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_customer_pending_validation
    ON customers(next_validation_at)
    WHERE validation_status = 'PENDING';

-- El estado de validacion es un conjunto explicito: pendiente, validado y
-- observado. "Observado" es la discrepancia que informa el hub; no es un
-- fallo de la validacion, que es otra cosa: un intento que se reintenta.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM customers WHERE validation_status NOT IN ('PENDING', 'VALID', 'OBSERVED')
    ) THEN
        RAISE EXCEPTION 'Hay clientes con un estado de validacion fuera del conjunto esperado';
    END IF;

    IF EXISTS (
        SELECT 1 FROM vehicles WHERE validation_status NOT IN ('PENDING', 'VALID', 'OBSERVED')
    ) THEN
        RAISE EXCEPTION 'Hay vehiculos con un estado de validacion fuera del conjunto esperado';
    END IF;
END
$$;
