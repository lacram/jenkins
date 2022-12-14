package cleanbook.com.service;

import cleanbook.com.dto.ResultDto;
import cleanbook.com.dto.page.CommentCreateDto;
import cleanbook.com.dto.page.CommentDto;
import cleanbook.com.entity.notification.NotificationType;
import cleanbook.com.entity.page.Comment;
import cleanbook.com.entity.page.Page;
import cleanbook.com.entity.user.User;
import cleanbook.com.exception.exceptions.CommentNotFoundException;
import cleanbook.com.exception.exceptions.NoAuthroizationException;
import cleanbook.com.exception.exceptions.PageNotFoundException;
import cleanbook.com.exception.exceptions.UserNotFoundException;
import cleanbook.com.jwt.TokenProvider;
import cleanbook.com.repository.comment.CommentRepository;
import cleanbook.com.repository.page.PageRepository;
import cleanbook.com.repository.user.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class CommentService {

    private final UserRepository userRepository;
    private final PageRepository pageRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    // λκΈ μμ±
    public void createComment(Long userId, CommentCreateDto dto) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Page page = pageRepository.findById(dto.getPageId()).orElseThrow(PageNotFoundException::new);
        User targetUser = userRepository.findById(page.getUser().getId()).orElseThrow(UserNotFoundException::new);
        Comment comment = Comment.createComment(user, page, dto.getContent(), dto.getGroup(), dto.isNested(), dto.isVisible());

        if (comment.isNested()) {
            int order = commentRepository.findFirstByGroupOrderByOrderDesc(dto.getGroup()).orElseThrow(CommentNotFoundException::new).getOrder();
            comment.setOrder(order+1);
        }

        commentRepository.save(comment);
        notificationService.send(user, targetUser, dto.isNested() ? NotificationType.NESTED : NotificationType.COMMENT);
    }

    // λκΈ μ‘°ν(ν κ²μκΈμ λκΈ μ μ²΄ μ‘°ν, λλκΈ μ μΈ, 10κ°μ©)
    public ResultDto<List<CommentDto>> readCommentList(Long pageId, Long startId) {
        return commentRepository.readCommentList(pageId, startId, 10);
    }

    // λλκΈ μ‘°ν(ν λκΈμ λκΈ μ‘°ν, 10κ°μ©)
    public ResultDto<List<CommentDto>> readNestedCommentList(Long pageId, int group, Long startId) {
        return commentRepository.readNestedCommentList(pageId, group, startId, 10);
    }

    // λκΈ μ­μ 
    public void deleteComment(Long userId, Long commentId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);

        if (!comment.getUser().getId().equals(userId)) {
            throw new NoAuthroizationException();
        }

        commentRepository.delete(comment);
    }
}
