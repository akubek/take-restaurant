package pl.polsl.take.restaurant.model.enums;

import lombok.Getter;

@Getter
/**
 * Measurement units used for ingredient quantities.
 */
public enum Unit {
    /** Gram unit. */
    GRAM("g"),
    /** Decagram unit. */
    DECAGRAM("dag"),
    /** Kilogram unit. */
    KILOGRAM("kg"),
    /** Milliliter unit. */
    MILLILITER("ml"),
    /** Deciliter unit. */
    DECILITER("dl"),
    /** Liter unit. */
    LITER("l"),
    /** Piece/count unit. */
    PIECE("szt.");

    /** Human-readable short symbol for the unit. */
    private final String symbol;

    /**
     * Creates a unit with its short symbol.
     *
     * @param symbol abbreviated form used in responses
     */
    Unit(String symbol) {
        this.symbol = symbol;
    }
}
