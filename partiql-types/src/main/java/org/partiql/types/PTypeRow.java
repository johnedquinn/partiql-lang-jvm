package org.partiql.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Applicable to {@link PType#ROW}.
 */
class PTypeRow extends PType {

    final Collection<Field> _fields;

    PTypeRow(@NotNull Collection<Field> fields) {
        super(PType.ROW);
        _fields = fields;
    }

    @NotNull
    @Override
    public Collection<Field> getFields() {
        return _fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        if (PType.ROW != ((PType) o).code()) {
            return false;
        }
        Collection<Field> otherFields = ((PType) o).getFields();
        int size = _fields.size();
        if (size != otherFields.size()) {
            return false;
        }
        Iterator<Field> thisIter = _fields.iterator();
        Iterator<Field> otherIter = otherFields.iterator();
        for (int i = 0; i < size; i++) {
            Field thisField = thisIter.next();
            Field otherField = otherIter.next();
            if (!thisField.equals(otherField)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        Collection<String> fieldStringList = _fields.stream().map((f) -> f.getName() + ": " + f.getType()).collect(Collectors.toList());
        String fieldStrings = String.join(", ", fieldStringList);
        return "ROW(" + fieldStrings + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(code(), _fields);
    }
}
