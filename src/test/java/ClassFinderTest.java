import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Komyshenets on 09.02.2018.
 */
public class ClassFinderTest {
    @Test
    public void constructorClassFinder() throws NoSuchFieldException, IllegalAccessException {
        String[] strings = {"", " "};
        for (String string : strings) {
            try {
                new ClassFinder(string);
                Assert.fail("Illegal pattern");
            } catch (IllegalArgumentException ignored) {
            }
        }

        Field pattern = ClassFinder.class.getDeclaredField("patternMasks");
        pattern.setAccessible(true);
        Assert.assertEquals(List.class, pattern.getType());

        Assert.assertEquals(((List) pattern.get(new ClassFinder("abcd"))).size(), 4);
        Assert.assertEquals(((List) pattern.get(new ClassFinder("FB"))).size(), 2);
        Assert.assertEquals(((List) pattern.get(new ClassFinder("FooB"))).size(), 2);
        Assert.assertEquals(((List) pattern.get(new ClassFinder("FoBa"))).size(), 2);
        Assert.assertEquals(((List) pattern.get(new ClassFinder("FoBZ"))).size(), 3);
        Assert.assertEquals(((List) pattern.get(new ClassFinder("Fo*BZ"))).size(), 3);
        Assert.assertEquals(((List) pattern.get(new ClassFinder("Fo*BZ "))).size(), 3);
    }

    @Test
    public void splitToWords() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassFinder classFinder = new ClassFinder("A");
        Method method = ClassFinder.class.getDeclaredMethod("splitToWords", String.class);
        method.setAccessible(true);
        Assert.assertEquals(List.class, method.getReturnType());

        Assert.assertEquals(((List) method.invoke(classFinder, "qqqWwwwEeee")).size(), 3);
        Assert.assertEquals(((List) method.invoke(classFinder, "qqqWwwwE")).size(), 3);
        Assert.assertEquals(((List) method.invoke(classFinder, "QqqqWw*wwEeee ")).size(), 3);
        Assert.assertEquals(((List) method.invoke(classFinder, "QqqqEWwwwEeee ")).size(), 4);
        Assert.assertEquals(((List) method.invoke(classFinder, "qwe")).size(), 1);
        Assert.assertEquals(((List) method.invoke(classFinder, "Qqqq")).size(), 1);
        Assert.assertEquals(((List) method.invoke(classFinder, "")).size(), 0);
        Assert.assertEquals(((List) method.invoke(classFinder, " ")).size(), 1);
    }

    @Test
    public void moveCursorAfterMask() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassFinder classFinder = new ClassFinder("A");
        Method method = ClassFinder.class.getDeclaredMethod("moveCursorAfterMask", String.class, String.class, int.class);
        method.setAccessible(true);
        Assert.assertEquals(int.class, method.getReturnType());

        Assert.assertEquals((int) method.invoke(classFinder, "F*r", "FooFozBor", 0), 9);
        Assert.assertEquals((int) method.invoke(classFinder, "F*rs", "FooFozBor", 0), -1);
        Assert.assertEquals((int) method.invoke(classFinder, "F*Foz*Bor", "FooFozBor", 0), -1);
        Assert.assertEquals((int) method.invoke(classFinder, "F*z*o*", "FooFozBor", 0), 9);
        Assert.assertEquals((int) method.invoke(classFinder, "F*z*o", "FooFozBor", 0), 8);
        Assert.assertEquals((int) method.invoke(classFinder, "*F", "FooFozBor", 0), 4);
        Assert.assertEquals((int) method.invoke(classFinder, "o*", "FooFozBor", 0), 3);
        Assert.assertEquals((int) method.invoke(classFinder, "**o*", "FooFozBor", 0), 4);

        Assert.assertEquals((int) method.invoke(classFinder, " ", "a", 0), -1);
        Assert.assertEquals((int) method.invoke(classFinder, "a", " ", 0), -1);
        Assert.assertEquals((int) method.invoke(classFinder, " ", " ", 0), 1);
        Assert.assertEquals((int) method.invoke(classFinder, "", "", 0), 0);
    }

    @Test
    public void match() {
        Map<String, Boolean> strings = new LinkedHashMap<>();
        strings.put("a.b.FooBarBaz", true);
        strings.put("c.d.FooBar", true);
        strings.put("c.d.FooRar", false);
        strings.put("c.d.BarFooRar", false);
        strings.put("FooBar", true);

        assertMatchStrings(new ClassFinder("FB"), strings);
        assertMatchStrings(new ClassFinder("FooB"), strings);
        assertMatchStrings(new ClassFinder("FoBa"), strings);

        Assert.assertTrue(new ClassFinder("FooBBa").match("a.b.FooBarBaz"));
    }

    private void assertMatchStrings(ClassFinder classFinder, Map<String, Boolean> strings) {
        for (Map.Entry<String, Boolean> entry : strings.entrySet()) {
            boolean match = classFinder.match(entry.getKey());
            if (entry.getValue()) {
                Assert.assertTrue(match);
            } else {
                Assert.assertFalse(match);
            }
        }
    }

    @Test
    public void matchWithSpace() {

        Assert.assertTrue(new ClassFinder("FB ").match("a.b.FooBarBaz"));
        Assert.assertTrue(new ClassFinder("FooB ").match("a.b.FooBarBaz"));
        Assert.assertTrue(new ClassFinder("FooBB ").match("a.b.FooBarBaz"));
        Assert.assertTrue(new ClassFinder("FooBBaz ").match("a.b.FooBarBaz"));
        Assert.assertTrue(new ClassFinder("FoBarBaz ").match("a.b.FooBarBaz"));
        Assert.assertTrue(new ClassFinder("arBazGaz ").match("a.b.FooBarBazGaz"));
        Assert.assertTrue(new ClassFinder("ooBBazGa ").match("a.b.FooBarBazGaz"));
        Assert.assertTrue(new ClassFinder("*** ").match("a.b.FooBarBazGaz"));
        Assert.assertTrue(new ClassFinder("** ").match("a.b.FooBarBazGaz"));

        Assert.assertFalse(new ClassFinder("FooBBaz ").match("a.b.FooBarBazGaz"));
        Assert.assertFalse(new ClassFinder("ooBBazGaza ").match("a.b.FooBarBazGaz"));
        Assert.assertFalse(new ClassFinder("fbb ").match("a.b.FooBarBazGaz"));
        Assert.assertFalse(new ClassFinder("**** ").match("a.b.FooBarBazGaz"));
    }

    @Test
    public void matchWithSpaceAndStars() {

        Assert.assertTrue(new ClassFinder("FB* ").match("a.b.FooBarBaz"));
        Assert.assertTrue(new ClassFinder("Foo*B ").match("a.b.FooBarBaz"));
        Assert.assertTrue(new ClassFinder("FooB*B** ").match("a.b.FooBarBaz"));
        Assert.assertTrue(new ClassFinder("FooBB*z ").match("a.b.FooBarBaz"));
        Assert.assertTrue(new ClassFinder("arBazGa* ").match("a.b.FooBarBazGaz"));

        Assert.assertFalse(new ClassFinder("FoBarB*az ").match("a.b.FooBarBaz"));
        Assert.assertFalse(new ClassFinder("ooBBaz*Ga ").match("a.b.FooBarBazGaz"));
        Assert.assertFalse(new ClassFinder("FooB*B*** ").match("a.b.FooBarBaz"));
        Assert.assertFalse(new ClassFinder("F*ooBBaz ").match("a.b.FooBarBazGaz"));
        Assert.assertFalse(new ClassFinder("o*oBBazGaz ").match("a.b.FooBarBazGaz"));
        Assert.assertFalse(new ClassFinder("fbb* ").match("a.b.FooBarBazGaz"));

        Assert.assertFalse(new ClassFinder("a").match(""));
    }

    @Test
    public void isLastWord() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassFinder classFinder = new ClassFinder("A");
        Method method = ClassFinder.class.getDeclaredMethod("isLastWord", String.class, String.class, int.class);
        method.setAccessible(true);
        Assert.assertEquals(boolean.class, method.getReturnType());

        Assert.assertFalse(((boolean) method.invoke(classFinder, "Eeee", "qqqWwwwEeee", 8)));
        Assert.assertTrue(((boolean) method.invoke(classFinder, "Eeee", "qqqWwwwEeee", 7)));
        Assert.assertTrue(((boolean) method.invoke(classFinder, "Eeee", "qqqWwwwEeee", 6)));

        Assert.assertFalse(((boolean) method.invoke(classFinder, "eeee", "qqqWwwwEeee", 6)));
        Assert.assertTrue(((boolean) method.invoke(classFinder, "eee", "qqqWwwwEeee", 6)));

        Assert.assertFalse(((boolean) method.invoke(classFinder, "Eeee", "qqqWwwwEee", 6)));
        Assert.assertTrue(((boolean) method.invoke(classFinder, "Eee", "qqqWwwwEeee", 6)));
        Assert.assertTrue(((boolean) method.invoke(classFinder, "*oob", "qqqWwwwFoobar", 6)));
        Assert.assertFalse(((boolean) method.invoke(classFinder, "*o*r*", "qqqWwwwFoobar", 6)));

        Assert.assertFalse(((boolean) method.invoke(classFinder, "Eee", "", 6)));
        Assert.assertFalse(((boolean) method.invoke(classFinder, "", "qqqWwwwFoobar", 6)));
        Assert.assertFalse(((boolean) method.invoke(classFinder, "", "", 6)));

    }


}