package com.github.zxh.classpy.dexfile.body.data.encoded;

import com.github.zxh.classpy.common.FileParseException;
import com.github.zxh.classpy.dexfile.DexComponent;
import com.github.zxh.classpy.dexfile.DexFile;
import com.github.zxh.classpy.dexfile.DexReader;
import com.github.zxh.classpy.dexfile.body.ids.FieldIdItem;
import com.github.zxh.classpy.dexfile.body.ids.MethodIdItem;
import com.github.zxh.classpy.dexfile.body.ids.TypeIdItem;
import com.github.zxh.classpy.dexfile.datatype.UByte;
import com.github.zxh.classpy.dexfile.helper.EncodedValueDecoder;
import java.io.IOException;

/**
 *
 * @author zxh
 */
public class EncodedValue extends DexComponent {

    private UByte typeAndArg; // (value_arg << 5) | value_type
    private DexComponent value;
    
    private int valueType;
    private int index;

    @Override
    protected void readContent(DexReader reader) {
        typeAndArg = reader.readUByte();
        try {
            decodeValue(typeAndArg, reader);
        } catch (IOException e) {
            throw new FileParseException(e);
        }
    }

    private void decodeValue(UByte typeAndArg, DexReader reader) throws IOException {
        valueType = typeAndArg.getValue() & 0b11111;
        int valueArg = typeAndArg.getValue() >> 5;
        int size = valueArg + 1;

        String typeName = getTypeName(valueType);
        setDesc(typeName);
        typeAndArg.setDesc(typeName + "&" + valueArg);

        switch (valueType) {
            case 0x00: // signed one-byte integer value
                value = reader.readByteArray(1);
                value.setDesc(new EncodedValueDecoder(value, 1, true).readByte());
                break;
            case 0x02: // signed two-byte integer value, sign-extended
                value = reader.readByteArray(size);
                value.setDesc(new EncodedValueDecoder(value, 2, true).readShort());
                break;
            case 0x03: // unsigned two-byte integer value, zero-extended
                value = reader.readByteArray(size);
                value.setDesc(new EncodedValueDecoder(value, 2, false).readChar());
                break;
            case 0x04: // signed four-byte integer value, sign-extended
                value = reader.readByteArray(size);
                value.setDesc(new EncodedValueDecoder(value, 4, true).readInt());
                break;
            case 0x06: // signed eight-byte integer value, sign-extended
                value = reader.readByteArray(size);
                value.setDesc(new EncodedValueDecoder(value, 8, true).readLong());
                break;
            case 0x10: // four-byte bit pattern, zero-extended to the right, and interpreted as an IEEE754 32-bit floating point value 
                value = reader.readByteArray(size);
                value.setDesc(new EncodedValueDecoder(value, 4, false).readFloat());
                break;
            case 0x11: // eight-byte bit pattern, zero-extended to the right, and interpreted as an IEEE754 64-bit floating point value 
                value = reader.readByteArray(size);
                value.setDesc(new EncodedValueDecoder(value, 8, false).readDouble());
                break;
            case 0x17: // unsigned (zero-extended) four-byte integer value, interpreted as an index into the string_ids section and representing a string value 
            case 0x18: // unsigned (zero-extended) four-byte integer value, interpreted as an index into the type_ids section and representing a reflective type/class value 
            case 0x19: // unsigned (zero-extended) four-byte integer value, interpreted as an index into the field_ids section and representing a reflective field value 
            case 0x1a: // unsigned (zero-extended) four-byte integer value, interpreted as an index into the method_ids section and representing a reflective method value 
            case 0x1b: // unsigned (zero-extended) four-byte integer value, interpreted as an index into the field_ids section and representing the value of an enumerated type constant 
                value = reader.readByteArray(size);
                index = new EncodedValueDecoder(value, 4, false).readInt();
                value.setDesc(index);
                break;
            case 0x1c: // an array of values, in the format specified by "encoded_array Format" below. The size of the value is implicit in the encoding. 
                value = new EncodedArray();
                value.read(reader);
                break;
            case 0x1d: // a sub-annotation, in the format specified by "encoded_annotation Format" below. The size of the value is implicit in the encoding. 
                value = new EncodedAnnotation();
                value.read(reader);
                break;
            case 0x1e: // null reference value
                break;
            case 0x1f: // one-bit value; 0 for false and 1 for true. The bit is represented in the value_arg. 
                break;
        }
    }

    private static String getTypeName(int valueType) {
        switch (valueType) {
            case 0x00: return "0x00(VALUE_BYTE)";
            case 0x02: return "0x02(VALUE_SHORT)";
            case 0x03: return "0x03(VALUE_CHAR)";
            case 0x04: return "0x04(VALUE_INT)";
            case 0x06: return "0x06(VALUE_LONG)";
            case 0x10: return "0x10(VALUE_FLOAT)";
            case 0x11: return "0x11(VALUE_DOUBLE)";
            case 0x17: return "0x17(VALUE_STRING)";
            case 0x18: return "0x18(VALUE_TYPE)";
            case 0x19: return "0x19(VALUE_FIELD)";
            case 0x1a: return "0x1a(VALUE_METHOD)";
            case 0x1b: return "0x1b(VALUE_ENUM)";
            case 0x1c: return "0x1c(VALUE_ARRAY)";
            case 0x1d: return "0x1d(VALUE_ANNOTATION)";
            case 0x1e: return "0x1e(VALUE_NULL)";
            case 0x1f: return "0x1f(VALUE_BOOLEAN)";
            default: throw new FileParseException("Invalid EncodedValue Type: " + valueType);
        }
    }

    @Override
    protected void postRead(DexFile dexFile) {
        super.postRead(dexFile);
        if (valueType == 0x17) { // VALUE_STRING
            value.setDesc(index + "->" + dexFile.getString(index));
        } else if (valueType == 0x18) { // VALUE_TYPE
            TypeIdItem typeId = dexFile.getTypeIdItem(index);
            String typeDesc = dexFile.getString(typeId.getDescriptorIdx());
            value.setDesc(index + "->" + typeDesc);
        } else if (valueType == 0x19 // VALUE_FIELD
                || valueType == 0x1b) { // VALUE_ENUM
            FieldIdItem fieldId = dexFile.getFieldIdItem(index);
            String fieldName = fieldId.getDesc();
            String className = dexFile.getTypeIdItem(fieldId.getClassIdx()).getDesc();
            value.setDesc(index + "->" + className + "." + fieldName);
        } else if (valueType == 0x1a) { // VALUE_METHOD
            MethodIdItem methodId = dexFile.getMethodIdItem(index);
            String methodName = methodId.getDesc();
            String className = dexFile.getTypeIdItem(methodId.getClassIdx()).getDesc();
            value.setDesc(index + "->" + className + "." + methodName);
        }
        // todo
    }

}
