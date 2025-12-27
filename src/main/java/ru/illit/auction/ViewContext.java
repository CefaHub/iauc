package ru.illit.auction;

/**
 * Контекст просмотра GUI для конкретного игрока.
 */
public final class ViewContext {
    /** Страница основного аукциона (0-based). */
    public int page = 0;

    /** Страница меню "Мои лоты" (0-based). */
    public int myPage = 0;

    /** Текущий режим сортировки. */
    public AuctionService.Sort sort = AuctionService.Sort.NEW_FIRST;

    /** Текст поиска (null = без фильтра). */
    public String search = null;
}
