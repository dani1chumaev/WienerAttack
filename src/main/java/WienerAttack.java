import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class WienerAttack {

    // Four ArrayList for finding proper p/q which later on for guessing k/dg
    List<BigInteger> d = new ArrayList<BigInteger>();
    List<Fraction> a = new ArrayList<Fraction>();
    List<BigInteger> p = new ArrayList<BigInteger>(); // k
    List<BigInteger> q = new ArrayList<BigInteger>(); // d
    Fraction kDdg = new Fraction(BigInteger.ZERO, BigInteger.ONE); // k/dg, D means "divide"
    private BigInteger e;
    private BigInteger N;

    /**
     * Attacks given public key and modulos.
     *
     * @param e
     *            public key
     * @param N
     *            modulos which needs to be factorized
     * @return
     */
    public BigInteger attack(BigInteger e, BigInteger N) {
        this.e = e;
        this.N = N;
        int i = 0;
        BigInteger temp1;

        // This loop keeps going unless the privateKey is calculated or no
        // privateKey is generated
        // When no privateKey is generated, temp1 == -1
        while ((temp1 = step(i)) == null) {
            i++;
        }

        return temp1;
    }

    public BigInteger step(int iteration) {
        if (iteration == 0) {
            // initialization for iteration 0
            Fraction ini = new Fraction(e, N);
            d.add(ini.floor());
            a.add(ini.remainder());
            p.add(d.get(0));
            q.add(BigInteger.ONE);
        } else if (iteration == 1) {
            // iteration 1
            Fraction temp2 = new Fraction(a.get(0).denominator, a.get(0).numerator);
            d.add(temp2.floor());
            a.add(temp2.remainder());
            p.add((d.get(0).multiply(d.get(1))).add(BigInteger.ONE));
            q.add(d.get(1));
        } else {
            if (a.get(iteration - 1).numerator.equals(BigInteger.ZERO)) {
                return BigInteger.ONE.negate();
                // Finite continued fraction.
                // and no proper privateKey
                // could be generated. Return -1
            }

            // go on calculating p and q for iteration i by using formulas
            // stating on the paper
            Fraction temp3 = new Fraction(a.get(iteration - 1).denominator, a.get(iteration - 1).numerator);
            d.add(temp3.floor());
           System.out.format("num %s\nden %s \n",temp3.numerator, temp3.denominator);
            a.add(temp3.remainder());
//            System.out.println("remainder " + temp3.remainder());
            p.add((d.get(iteration).multiply(p.get(iteration - 1)).add(p.get(iteration - 2))));
            q.add((d.get(iteration).multiply(q.get(iteration - 1)).add(q.get(iteration - 2))));
        }

        System.out.format("iter %s {\n d %s\n a %s\n p %s\n q %s\n}\n", iteration, d.get(iteration), a.get(iteration), p.get(iteration), q.get(iteration));

        // if iteration is even, assign <q0, q1, q2,...,qi+1> to kDdg
        if (iteration % 2 == 0) {
            if (iteration == 0) {
                kDdg = new Fraction(d.get(0).add(BigInteger.ONE), BigInteger.ONE);
            } else {
                kDdg = new Fraction((d.get(iteration).add(BigInteger.ONE)).multiply(p.get(iteration - 1)).add(
                        p.get(iteration - 2)), (d.get(iteration).add(BigInteger.ONE)).multiply(q.get(iteration - 1))
                        .add(q.get(iteration - 2)));
            }
        }
        // if iteration is odd, assign <q0, q1, q2,...,qi> to kDdg
        else {
            kDdg = new Fraction(p.get(iteration), q.get(iteration));
        }

//        System.out.println(kDdg);

        // from formula phi = (ed - 1) / k
        BigInteger phi = e.multiply(kDdg.denominator).subtract(BigInteger.ONE).divide(kDdg.numerator);
        // p + q = N - phi + 1
        BigInteger b = N.subtract(phi).add(BigInteger.ONE);
        BigInteger c = N;
        // d = b^2 - (4 * c)
        BigInteger descriminant = b.multiply(b).subtract(new BigInteger("4").multiply(c));
        // |(-b + sqrt(d)) / 2|
        BigInteger x1 = b.negate().add(descriminant.sqrt()).divide(new BigInteger("2")).abs();
        // |(-b - sqrt(d)) / 2|
        BigInteger x2 = b.negate().subtract(descriminant.sqrt()).divide(new BigInteger("2")).abs();

        // x1 * x2 = N
        if (x1.multiply(x2).equals(N)) {
            // d = e^-1 mod phi
            return e.modInverse(phi);
        }

        // edg = e * dg
        BigInteger edg = this.e.multiply(kDdg.denominator);
        // dividing edg by k yields a quotient of (p-1)(d-1) and a remainder of g
        // (e * dg / gcd) / (k / gcd)
        BigInteger fy = (new Fraction(this.e, kDdg)).floor();
        BigInteger g = edg.mod(kDdg.numerator);
        // get (p+d)/2 and check whether (p+d)/2 is integer or not
        // (N - fy + 1) / 2
        BigDecimal pAqD2 = (new BigDecimal(this.N.subtract(fy))).add(BigDecimal.ONE).divide(new BigDecimal("2"));
        if (!pAqD2.remainder(BigDecimal.ONE).equals(BigDecimal.ZERO))
            return null;
        // get [(p-d)/2]^2 and check [(p-d)/2]^2 is a perfect square or not
        // ((p+d)/2)^2 - N
        BigInteger pMqD2s = pAqD2.toBigInteger().pow(2).subtract(N);
        BigInteger pMqD2 = pMqD2s.sqrt();
        if (!pMqD2.pow(2).equals(pMqD2s))
            return null;
        // get private key q from edg/eg
        BigInteger privateKey = edg.divide(e.multiply(g));
        return privateKey;

    }
}
