(function (global) {
    function _create_pollyfill(obj, key, value) {
        if (!obj[key]) obj[key] = value;
        return obj[key];
    }

    if (typeof global.Symbol !== "function") {
        var idCounter = 0;
        var AllSymbols = {};
        function Symbol(description) {
            if (this instanceof Symbol)
                throw new TypeError("Symbol is not a constructor");
            var desc = description === undefined ? "" : String(description);
            var tag = "@@Symbol(" + desc + "):" + idCounter++;
            var sym = Object.create(null);
            sym.toString = function () {
                return tag;
            };
            AllSymbols[tag] = sym;
            return sym;
        }
        _create_pollyfill(Symbol, "for", function (key) {
            if (!this._registry) this._registry = {};
            if (!this._registry[key]) this._registry[key] = Symbol(key);
            return this._registry[key];
        });
        _create_pollyfill(Symbol, "keyFor", function (sym) {
            for (var key in this._registry) {
                if (this._registry[key] === sym) return key;
            }
            return undefined;
        });
        Symbol.iterator = Symbol("Symbol.iterator");
        global.Symbol = Symbol;
    }

    if (!Array.prototype[Symbol.iterator]) {
        Object.defineProperty(Array.prototype, Symbol.iterator, {
            configurable: true,
            writable: true,
            value: function () {
                var i = 0,
                    self = this;
                return {
                    next: function () {
                        if (i < self.length) {
                            return { value: self[i++], done: false };
                        } else {
                            return { done: true };
                        }
                    },
                };
            },
        });
    }

    _create_pollyfill(Array.prototype, Symbol.iterator, function () {
        var i = 0,
            self = this;
        return {
            next: function next() {
                return i < self.length
                    ? {
                          value: self[i++],
                          done: false,
                      }
                    : {
                          done: true,
                      };
            },
        };
    });
    ["java.util.List", "java.util.Map", "java.util.Set"].forEach(function (
        cls
    ) {
        try {
            var JavaCls = Java.type(cls);
            if (JavaCls && !JavaCls.prototype[Symbol.iterator]) {
                _create_pollyfill(
                    JavaCls.prototype,
                    Symbol.iterator,
                    function () {
                        var it =
                            cls === "java.util.Map"
                                ? this.entrySet().iterator()
                                : this.iterator();
                        return {
                            next: function next() {
                                if (it.hasNext()) {
                                    if (cls === "java.util.Map") {
                                        var e = it.next();
                                        return {
                                            value: {
                                                key: e.getKey(),
                                                value: e.getValue(),
                                            },
                                            done: false,
                                        };
                                    }
                                    return {
                                        value: it.next(),
                                        done: false,
                                    };
                                } else
                                    return {
                                        done: true,
                                    };
                            },
                        };
                    }
                );
            }
        } catch (e) {}
    });
    _create_pollyfill(Object, "from", function (obj) {
        if (obj == null) return obj;
        try {
            var JavaMap = Java.type("java.util.Map");
            var JavaList = Java.type("java.util.List");
            var JavaArray = Java.type("java.lang.reflect.Array");
            if (obj instanceof JavaMap) {
                var result = {};
                var it = obj.entrySet().iterator();
                while (it.hasNext()) {
                    var e = it.next();
                    result[e.getKey()] = Object.from(e.getValue());
                }
                return result;
            }
            if (obj instanceof JavaList) {
                var arr = [];
                for (var i = 0; i < obj.size(); i++)
                    arr.push(Object.from(obj.get(i)));
                return arr;
            }
            if (obj.getClass && obj.getClass().isArray()) {
                var len = JavaArray.getLength(obj);
                var arr = [];
                for (var i = 0; i < len; i++)
                    arr.push(Object.from(JavaArray.get(obj, i)));
                return arr;
            }
        } catch (e) {}
        return obj;
    });
    _create_pollyfill(Object, "keys", function (obj) {
        var res = [];
        for (var key in obj) if (obj.hasOwnProperty(key)) res.push(key);
        return res;
    });
    _create_pollyfill(Object, "values", function (obj) {
        var res = [];
        for (var key in obj) if (obj.hasOwnProperty(key)) res.push(obj[key]);
        return res;
    });
    _create_pollyfill(Object, "entries", function (obj) {
        var res = [];
        for (var key in obj)
            if (obj.hasOwnProperty(key)) res.push([key, obj[key]]);
        return res;
    });
    _create_pollyfill(Object, "assign", function (target) {
        if (target == null)
            throw new TypeError("Cannot convert undefined or null to object");
        target = Object(target);
        for (var i = 1; i < arguments.length; i++) {
            var src = arguments[i];
            if (src != null) {
                for (var key in src) {
                    if (src.hasOwnProperty(key)) target[key] = src[key];
                }
            }
        }
        return target;
    });
    _create_pollyfill(Array.prototype, "flat", function (depth) {
        depth = depth === undefined ? 1 : depth;
        function flatten(arr, d) {
            if (d < 1) return arr.slice();
            var res = [];
            arr.forEach(function (item) {
                if (Array.isArray(item)) res = res.concat(flatten(item, d - 1));
                else res.push(item);
            });
            return res;
        }
        return flatten(this, depth);
    });
    _create_pollyfill(Array.prototype, "flatMap", function (fn) {
        return this.map(fn).flat();
    });
    _create_pollyfill(Array.prototype, "find", function (fn) {
        for (var i = 0; i < this.length; i++)
            if (fn(this[i], i, this)) return this[i];
        return undefined;
    });
    _create_pollyfill(Array.prototype, "findIndex", function (fn) {
        for (var i = 0; i < this.length; i++)
            if (fn(this[i], i, this)) return i;
        return -1;
    });
    _create_pollyfill(Array.prototype, "includes", function (val) {
        for (var i = 0; i < this.length; i++) if (this[i] === val) return true;
        return false;
    });
    _create_pollyfill(Array.prototype, "some", function (fn) {
        for (var i = 0; i < this.length; i++)
            if (fn(this[i], i, this)) return true;
        return false;
    });
    _create_pollyfill(Array.prototype, "every", function (fn) {
        for (var i = 0; i < this.length; i++)
            if (!fn(this[i], i, this)) return false;
        return true;
    });
    _create_pollyfill(Array.prototype, "fill", function (val, start, end) {
        start = start || 0;
        end = end === undefined ? this.length : end;
        for (var i = start; i < end; i++) this[i] = val;
        return this;
    });
    _create_pollyfill(
        Array.prototype,
        "copyWithin",
        function (target, start, end) {
            target = target | 0;
            start = start | 0;
            end = end === undefined ? this.length : end | 0;
            var len = this.length;
            target =
                target < 0 ? Math.max(len + target, 0) : Math.min(target, len);
            start = start < 0 ? Math.max(len + start, 0) : Math.min(start, len);
            end = end < 0 ? Math.max(len + end, 0) : Math.min(end, len);
            var count = Math.min(end - start, len - target);
            if (start < target && target < start + count) {
                for (var i = count - 1; i >= 0; i--)
                    this[target + i] = this[start + i];
            } else {
                for (var i = 0; i < count; i++)
                    this[target + i] = this[start + i];
            }
            return this;
        }
    );
    _create_pollyfill(Array, "from", function (obj) {
        if (obj == null) return [];
        return Java.from(obj);
    });
    _create_pollyfill(Array, "of", function () {
        return Array.prototype.slice.call(arguments);
    });
})(this);
