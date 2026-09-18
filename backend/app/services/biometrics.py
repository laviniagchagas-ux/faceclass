"""Assinatura geométrica facial usada no protótipo FaceClass.

O celular extrai 12 coordenadas normalizadas de seis pontos faciais usando o
ML Kit. Este serviço armazena a assinatura e compara a nova leitura sem gravar
foto, vídeo ou Base64 no banco. É apropriado para demonstração acadêmica; uma
implantação escolar real deve usar um modelo biométrico homologado, avaliação
de fraude e regras formais de consentimento e proteção de dados.
"""

from __future__ import annotations

from hashlib import sha256
import json
import math

SIGNATURE_LENGTH = 12
# Distância RMS máxima para a mesma face no cenário controlado da demonstração.
MAX_SIGNATURE_DISTANCE = 0.075


def normalize_signature(values: list[float]) -> list[float]:
    if len(values) != SIGNATURE_LENGTH:
        raise ValueError("A assinatura facial deve ter 12 medidas.")
    normalized = [round(float(value), 4) for value in values]
    if any(not math.isfinite(value) for value in normalized):
        raise ValueError("A assinatura facial contém valores inválidos.")
    return normalized


def serialize_signature(values: list[float]) -> str:
    return json.dumps(normalize_signature(values), separators=(",", ":"))


def deserialize_signature(value: str | None) -> list[float] | None:
    if not value:
        return None
    try:
        decoded = json.loads(value)
        if not isinstance(decoded, list):
            return None
        return normalize_signature(decoded)
    except (TypeError, ValueError, json.JSONDecodeError):
        return None


def signature_hash(values: list[float]) -> str:
    canonical = serialize_signature(values).encode("utf-8")
    return sha256(canonical).hexdigest()


def compare_signatures(reference: list[float], candidate: list[float]) -> tuple[bool, float]:
    reference = normalize_signature(reference)
    candidate = normalize_signature(candidate)
    distance = math.sqrt(
        sum((first - second) ** 2 for first, second in zip(reference, candidate))
        / SIGNATURE_LENGTH
    )
    return distance <= MAX_SIGNATURE_DISTANCE, round(distance, 4)
