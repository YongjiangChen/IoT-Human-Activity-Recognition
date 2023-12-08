// Autogenerated AST node
package org.python.antlr.base;

import org.antlr.runtime.Token;
import org.python.antlr.AST;
import org.python.antlr.PythonTree;
import org.python.antlr.base.slice;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

@ExposedType(name = "_ast.slice", base = AST.class)
public abstract class slice extends PythonTree {

    public static final PyType TYPE = PyType.fromClass(slice.class);
    private final static PyString[] fields = new PyString[0];
    @ExposedGet(name = "_fields")
    public PyString[] get_fields() { return fields; }

    private final static PyString[] attributes = new PyString[0];
    @ExposedGet(name = "_attributes")
    public PyString[] get_attributes() { return attributes; }

    public slice() {
    }

    public slice(PyType subType) {
    }

    public slice(int ttype, Token token) {
        super(ttype, token);
    }

    public slice(Token token) {
        super(token);
    }

    public slice(PythonTree node) {
        super(node);
    }

}