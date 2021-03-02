package de.gtrefs.coffeeshop.barista;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.*;

import org.hibernate.annotations.*;

@Entity
@Table(name = "tbl_cup")
public class Cup implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GenericGenerator(name="GivenOrGenerated", strategy = "de.gtrefs.coffeeshop.barista.UseGivenIdOrElseGenerateIt")
    @GeneratedValue(generator = "GivenOrGenerated")
    @Column(unique = true, nullable = false)
    private Long id;

	@Pattern(regexp = "melange|black|espresso|ristretto|cappuccino",
			flags = Pattern.Flag.CASE_INSENSITIVE,
			message = "We don't offer ${validatedValue}. Please pick one of " +
					"Black Coffee, Melange, Espresso, Ristretto or Cappuccino.")
	@NotNull(message = "Please order something.")
	private String flavor;

	public Cup() {
	}

	public static Cup of(String flavor) {
		Cup cup = new Cup();
		cup.setFlavor(flavor);
		return cup;
	}

	public Cup(String flavor){
		this.flavor = flavor;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFlavor() {
		return flavor;
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	@Override
	public String toString() {
		return "Cup{" +
				"id=" + id +
				", flavor='" + flavor + '\'' +
				'}';
	}
}