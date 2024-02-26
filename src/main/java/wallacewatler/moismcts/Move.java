package wallacewatler.moismcts;

/**
 *
 * @since 0.1
 *
 * @author Wallace Watler
 *
 * @param <ACTION> T
 * @param <INFO_SET>
 */
public interface Move<ACTION, INFO_SET> {
    /**
     *
     * @param infoSet
     */
    INFO_SET applyToInfoSet(INFO_SET infoSet);

    /**
     * Only defined for singleton moves; i.e., moves that contain only one action.
     */
    ACTION asAction();
}
